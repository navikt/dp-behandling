package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.januar
import no.nav.dagpenger.behandling.mediator.repository.Meldekortgenerator.Companion.generatorFor
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
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.hours

class MeldekortRepositoryPostgresTest {
    @Test
    fun `lagre og hente meldekort`() {
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

            val meldingsreferanseId1 = UUIDv7.ny()
            val meldekort =
                Meldekort(
                    id = UUIDv7.ny(),
                    meldingsreferanseId = meldingsreferanseId1,
                    ident = ident,
                    eksternMeldekortId = 1,
                    fom = start,
                    tom = start.plusDays(14),
                    kilde = MeldekortKilde("Bruker", ident),
                    dager = dager,
                    innsendtTidspunkt = LocalDateTime.now(),
                    korrigeringAv = null,
                )
            val meldekortInnsendtHendelse =
                MeldekortInnsendtHendelse(
                    opprettet = LocalDateTime.now(),
                    meldingsreferanseId = meldingsreferanseId1,
                    meldekort =
                    meldekort,
                )

            val meldingsreferanseId2 = UUIDv7.ny()
            val meldekortKorrigertInnsendtHendelse =
                MeldekortInnsendtHendelse(
                    opprettet = LocalDateTime.now(),
                    meldingsreferanseId = meldingsreferanseId2,
                    meldekort =
                        meldekort.copy(
                            id = UUIDv7.ny(),
                            meldingsreferanseId = meldingsreferanseId2,
                            eksternMeldekortId = 2,
                            korrigeringAv = meldekort.eksternMeldekortId,
                        ),
                )

            lagreHendelseOmMeldekort(ident, meldekortInnsendtHendelse)
            lagreHendelseOmMeldekort(ident, meldekortKorrigertInnsendtHendelse)

            meldekortRepository.lagre(meldekortInnsendtHendelse.meldekort)
            meldekortRepository.lagre(meldekortKorrigertInnsendtHendelse.meldekort)

            val rehydrertMeldekort = meldekortRepository.hent(meldekortInnsendtHendelse.meldekort.id)
            rehydrertMeldekort.shouldNotBeNull()
            rehydrertMeldekort.copy(innsendtTidspunkt = rehydrertMeldekort.innsendtTidspunkt.truncateToSeconds()) shouldBeEqual
                meldekortInnsendtHendelse.meldekort.copy(
                    innsendtTidspunkt = meldekort.innsendtTidspunkt.truncateToSeconds(),
                )

            val rehydrertKorrigertMeldekort = meldekortRepository.hent(meldekortKorrigertInnsendtHendelse.meldekort.id)
            rehydrertKorrigertMeldekort.shouldNotBeNull()
            rehydrertKorrigertMeldekort.copy(
                innsendtTidspunkt = rehydrertKorrigertMeldekort.innsendtTidspunkt.truncateToSeconds(),
            ) shouldBeEqual
                meldekortKorrigertInnsendtHendelse.meldekort.copy(
                    innsendtTidspunkt =
                        meldekortKorrigertInnsendtHendelse.meldekort.innsendtTidspunkt.truncateToSeconds(),
                )
        }
    }

    private fun LocalDateTime.truncateToSeconds() = this.truncatedTo(ChronoUnit.SECONDS)

    private fun lagreHendelseOmMeldekort(
        ident: String,
        meldekortInnsendtHendelse: MeldekortInnsendtHendelse,
    ) {
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

    @Test
    fun hentUbehandledeMeldekort() {
        withMigratedDb {
            val repo = MeldekortRepositoryPostgres()
            val meldingGenerator = Meldekortgenerator.meldingGenerator

            val person1 = repo.generatorFor("111111111", 1.januar(2024), meldingGenerator)
            val person2 = repo.generatorFor("222222222", 1.januar(2024), meldingGenerator)

            person1.lagMeldekort(10)
            person2.lagMeldekort(10)

            person2.markerFerdig(1)
            person2.markerFerdig(2)
            person2.markerFerdig(3)
            person2.lagKorrigering(4) {
                listOf()
            }

            with(repo.hentUbehandledeMeldekort()) {
                shouldHaveSize(2)
                map { it.meldekort.eksternMeldekortId } shouldContainInOrder listOf(1L, 21L)
            }

            person2.markerFerdig(11)
            with(repo.hentUbehandledeMeldekort()) {
                shouldHaveSize(2)
                map { it.meldekort.eksternMeldekortId } shouldContainInOrder listOf(1L, 15L)
            }

            person2.markerStartet(5)
            with(repo.hentUbehandledeMeldekort()) {
                shouldHaveSize(2)
                map { it.meldekort.eksternMeldekortId } shouldContainInOrder listOf(1L, 15L)
            }
        }
    }
}
