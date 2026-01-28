package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.uuid.UUIDv7
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.sql.DataSource

class Meldekortgenerator private constructor(
    private val repository: MeldekortRepository,
    val ident: String,
    private val eksternMeldekortId: Iterator<Long>,
    startdato: LocalDate = LocalDate.now(),
) {
    private val meldeperiode = periodeGenerator(startdato)
    private val innsendteMeldekort = mutableListOf<Meldekort>()

    fun lagKorrigering(
        dataSource: DataSource,
        i: Int,
        block: () -> List<Dag>,
    ): Boolean =
        innsendteMeldekort[i - 1]
            .let { meldekort ->
                val korrigertMeldekort =
                    meldekort
                        .copy(
                            id = UUIDv7.ny(),
                            meldingsreferanseId = UUIDv7.ny(),
                            eksternMeldekortId = MeldekortId(eksternMeldekortId.next().toString()),
                            korrigeringAv = meldekort.eksternMeldekortId,
                            dager = block(),
                        )
                lagreHendelseOmMeldekort(dataSource, korrigertMeldekort)
                innsendteMeldekort.add(korrigertMeldekort)
            }

    fun lagMeldekort(
        dataSource: DataSource,
        antall: Int = 1,
    ) {
        repeat(antall) {
            lagreHendelseOmMeldekort(dataSource, meldekort())
        }
    }

    fun markerStartet(i: Int) {
        val meldekort = innsendteMeldekort[i - 1]
        repository.behandlingStartet(meldekort.eksternMeldekortId)
    }

    fun markerFerdig(i: Int) {
        val meldekort = innsendteMeldekort[i - 1]
        repository.behandlingStartet(meldekort.eksternMeldekortId)
        repository.markerSomFerdig(meldekort.eksternMeldekortId)
    }

    fun meldekort(nummer: Int): Meldekort = innsendteMeldekort[nummer - 1]

    private fun meldekort(): Meldekort {
        val meldingsreferanseId = UUIDv7.ny()
        val periode = meldeperiode.next()

        val meldekort =
            Meldekort(
                id = UUIDv7.ny(),
                meldingsreferanseId = meldingsreferanseId,
                ident = ident,
                eksternMeldekortId = MeldekortId(eksternMeldekortId.next().toString()),
                fom = periode.start,
                tom = periode.endInclusive,
                kilde = MeldekortKilde("Bruker", ident),
                dager = listOf(),
                innsendtTidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                korrigeringAv = null,
                meldedato = periode.endInclusive.minusDays(1),
                kanSendesFra = periode.endInclusive.minusDays(1),
            ).also {
                this.innsendteMeldekort.add(it)
            }

        return meldekort
    }

    private fun lagreHendelseOmMeldekort(
        dataSource: DataSource,
        meldekort: Meldekort,
    ) {
        val meldekortInnsendtHendelse =
            MeldekortInnsendtHendelse(
                opprettet = LocalDateTime.now(),
                meldingsreferanseId = meldekort.meldingsreferanseId,
                meldekort = meldekort,
            )
        sessionOf(dataSource).use { session ->
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

    private fun periodeGenerator(startdato: LocalDate) =
        iterator {
            var sisteDato = startdato
            while (true) {
                yield(sisteDato..(sisteDato.plusDays(13)))
                sisteDato = sisteDato.plusDays(14)
            }
        }

    companion object {
        fun MeldekortRepository.generatorFor(
            ident: String,
            startdato: LocalDate,
            generator: Iterator<Long> = meldekortIdGenerator,
        ) = Meldekortgenerator(this, ident, generator, startdato)

        val meldekortIdGenerator
            get() =
                iterator {
                    var meldekortId = 1L
                    while (true) yield(meldekortId++)
                }
    }
}
