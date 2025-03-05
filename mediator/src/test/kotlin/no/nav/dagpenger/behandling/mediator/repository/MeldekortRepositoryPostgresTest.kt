package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.hours

class MeldekortRepositoryPostgresTest {
    @Test
    fun `lagre meldekort`() {
        withMigratedDb {
            val meldekortRepository = MeldekortRepositoryPostgres()
            val ident = "12345678910"
            val start = LocalDate.now()
            val dager =
                (1..14).map {
                    Dag(
                        dato = start.plusDays(it.toLong()),
                        meldt = true,
                        aktiviteter =
                            if (it % 2 == 0) {
                                listOf(
                                    MeldekortAktivitet(
                                        type = AktivitetType.Arbeid,
                                        timer = 2.hours,
                                    ),
                                )
                            } else {
                                listOf(
                                    MeldekortAktivitet(
                                        type = AktivitetType.Fravær,
                                        timer = null,
                                    ),
                                )
                            },
                    )
                }

            val meldingsreferanseId = UUIDv7.ny()
            val meldekortInnsendtHendelse =
                MeldekortInnsendtHendelse(
                    opprettet = LocalDateTime.now(),
                    meldingsreferanseId = meldingsreferanseId,
                    meldekort =
                        Meldekort(
                            id = UUIDv7.ny(),
                            meldingsreferanseId = meldingsreferanseId,
                            ident = ident,
                            eksternMeldekortId = 1,
                            fom = start,
                            tom = start.plusDays(14),
                            kilde = MeldekortKilde("Bruker", ident),
                            dager = dager,
                            innsendtTidspunkt = LocalDateTime.now(),
                            korrigeringAv = null,
                        ),
                )

            lagreHendelseOmMeldekort(ident, meldekortInnsendtHendelse)

            meldekortRepository.lagre(meldekortInnsendtHendelse.meldekort)

            val aktiviteter =
                sessionOf(dataSource).use {
                    it.run(
                        queryOf(
                            //language=PostgreSQL
                            """
                            SELECT COUNT(*) FROM meldekort_aktivitet
                            """.trimIndent(),
                        ).map {
                            it.int("count")
                        }.asSingle,
                    )
                }
            aktiviteter.shouldBe(14)
        }
    }

    private fun lagreHendelseOmMeldekort(
        ident: String,
        meldekortInnsendtHendelse: MeldekortInnsendtHendelse,
    ) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO person (ident) VALUES (:ident)                       
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident,
                    ),
                ).asUpdate,
            )
        }
        sessionOf(dataSource).use { session ->
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
    }
}
