package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.melding.PostgresMeldingRepository
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import org.junit.jupiter.api.Test
import java.util.UUID

class KildeRepositoryTest {
    private val meldingsreferanseId = UUID.randomUUID()
    private val kildeRepository = KildeRepository()

    init {
        PostgresMeldingRepository().also {
            it.lagreMelding(
                mockk(),
                "123456789",
                meldingsreferanseId,
                "{}",
            )
        }
    }

    @Test
    fun lagreBegrunnelse() =
        withMigratedDb {
            val kilde = Saksbehandlerkilde(meldingsreferanseId, Saksbehandler("EIF2025"))

            kildeRepository.lagreKilde(kilde, sessionOf(dataSource))
            kildeRepository.lagreBegrunnelse(kilde.id, "Begrunnelse")

            with(kildeRepository.hentKilde(kilde.id)) {
                shouldBeInstanceOf<Saksbehandlerkilde>()

                begrunnelse?.verdi shouldBe "Begrunnelse"
            }
        }

    @Test
    fun `Lagre kilde uten begrunnelse`() {
        withMigratedDb {
            val kilde = Saksbehandlerkilde(meldingsreferanseId, Saksbehandler("EIF2025"))

            kildeRepository.lagreKilde(kilde, sessionOf(dataSource))

            // Lagre en tom begrunnelse uten begrunnelse_sist_endret
            sessionOf(dataSource)
                .use { session ->
                    session.run(
                        queryOf(
                            //language=PostgreSQL
                            """
                            UPDATE kilde_saksbehandler 
                            SET begrunnelse = :begrunnelse 
                            WHERE kilde_id = :kildeId
                            """.trimIndent(),
                            mapOf(
                                "kildeId" to kilde.id,
                                "begrunnelse" to "",
                            ),
                        ).asUpdate,
                    )
                }

            with(kildeRepository.hentKilde(kilde.id)) {
                shouldBeInstanceOf<Saksbehandlerkilde>()

                shouldBeInstanceOf<Saksbehandlerkilde>().saksbehandler.ident shouldBe "EIF2025"
            }
        }
    }
}
