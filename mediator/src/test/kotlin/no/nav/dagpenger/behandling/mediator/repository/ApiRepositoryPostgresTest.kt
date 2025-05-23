package no.nav.dagpenger.behandling.mediator.repository

import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType
import org.junit.jupiter.api.Test
import java.util.UUID

class ApiRepositoryPostgresTest {
    @Test
    fun `kan vente på endringer i tilstand`() {
        withMigratedDb {
            val behandling = Behandling(UUID.randomUUID())
            behandling.endreTilstand(TilstandType.ForslagTilVedtak)

            val repo = ApiRepositoryPostgres(mockk())

            runBlocking {
                repo.endreOpplysning(
                    behandling.id,
                    "Fødselsdato",
                ) {
                    // Simulerer mottak av melding fra Kafka.
                    behandling.endreTilstand(TilstandType.Redigert)

                    // Simulerer en asynkron prosess som tar litt tid.
                    CoroutineScope(IO).launch {
                        delay(200)
                        behandling.endreTilstand(TilstandType.ForslagTilVedtak)
                    }
                }
            }
        }
    }

    private class Behandling(
        val id: UUID,
    ) {
        fun endreTilstand(tilstand: TilstandType) =
            sessionOf(dataSource).use {
                it.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        INSERT INTO behandling(behandling_id, tilstand)
                        VALUES (:id, :tilstand)
                        ON CONFLICT (behandling_id) DO UPDATE SET tilstand = :tilstand, sist_endret_tilstand = NOW()
                        """.trimIndent(),
                        mapOf(
                            "id" to id,
                            "tilstand" to tilstand.name,
                        ),
                    ).asUpdate,
                )
            }
    }
}
