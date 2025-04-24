package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.uuid.UUIDv7
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.time.LocalDateTime

class Meldekortgenerator private constructor(
    private val repository: MeldekortRepository,
    private val ident: String,
    private val eksternMeldekortId: Iterator<Long>,
    startdato: LocalDate = LocalDate.now(),
) {
    companion object {
        private val eksternMelding = iterator { yieldAll(1L..1000L) }

        fun MeldekortRepository.generatorFor(
            ident: String,
            startdato: LocalDate,
        ) = Meldekortgenerator(this, ident, eksternMelding, startdato)
    }

    private val meldeperiode = Meldeperiode(startdato)
    private val innsendteMeldekort = mutableListOf<Meldekort>()

    fun lagKorrigering(
        index: Int,
        block: () -> List<Dag>,
    ) {
        innsendteMeldekort[index - 1]
            .let { meldekort ->
                val korrigertMeldekort =
                    meldekort
                        .copy(
                            id = UUIDv7.ny(),
                            meldingsreferanseId = UUIDv7.ny(),
                            eksternMeldekortId = eksternMeldekortId.next(),
                            korrigeringAv = meldekort.eksternMeldekortId,
                            dager = block(),
                        ).also {
                            innsendteMeldekort.add(it)
                        }
                lagreHendelseOmMeldekort(korrigertMeldekort)
            }
    }

    fun lagMeldekort(antall: Int = 1) {
        repeat(antall) {
            lagreHendelseOmMeldekort(meldekort())
        }
    }

    fun markerFerdig(i: Int) {
        val meldekort = innsendteMeldekort[i - 1]
        repository.behandlingStartet(meldekort.eksternMeldekortId)
        repository.behandlet(meldekort.eksternMeldekortId)
    }

    fun markerStartet(i: Int) {
        val meldekort = innsendteMeldekort[i - 1]
        repository.behandlingStartet(meldekort.eksternMeldekortId)
    }

    private fun meldekort(): Meldekort {
        val meldingsreferanseId = UUIDv7.ny()
        val periode = meldeperiode.periode()

        val meldekort =
            Meldekort(
                id = UUIDv7.ny(),
                meldingsreferanseId = meldingsreferanseId,
                ident = ident,
                eksternMeldekortId = eksternMeldekortId.next(),
                fom = periode.start,
                tom = periode.endInclusive,
                kilde = MeldekortKilde("Bruker", ident),
                dager = listOf(),
                innsendtTidspunkt = LocalDateTime.now(),
                korrigeringAv = null,
            ).also {
                this.innsendteMeldekort.add(it)
            }

        return meldekort
    }

    private fun lagreHendelseOmMeldekort(meldekort: Meldekort) {
        val meldekortInnsendtHendelse =
            MeldekortInnsendtHendelse(
                opprettet = LocalDateTime.now(),
                meldingsreferanseId = meldekort.meldingsreferanseId,
                meldekort = meldekort,
            )
        sessionOf(PostgresDataSourceBuilder.dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO person (ident) VALUES (:ident) ON CONFLICT DO NOTHING                    
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident,
                    ),
                ).asUpdate,
            )
        }
        sessionOf(PostgresDataSourceBuilder.dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO melding
                         (ident, melding_id, melding_type, data, lest_dato)
                     VALUES
                         (:ident, :melding_id, :melding_type, :data, NOW())
                     ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident,
                        "melding_id" to meldekortInnsendtHendelse.meldingsreferanseId(),
                        "melding_type" to "Meldekort",
                        "data" to
                            PGobject().apply {
                                type = "json"
                                value = "{}"
                            },
                        "opprettet" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }

        repository.lagre(meldekortInnsendtHendelse.meldekort)
    }

    private class Meldeperiode(
        start: LocalDate,
    ) {
        private var aktiv = start

        fun periode(): ClosedRange<LocalDate> =
            aktiv..aktiv.plusDays(13).also {
                aktiv = it.plusDays(1)
            }
    }
}
