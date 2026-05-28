package no.nav.dagpenger.mediator.repository

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.OppdateringObserver
import no.nav.dagpenger.mediator.api.oppdateringerForBehandling
import no.nav.dagpenger.mediator.db.withIsolatedDb
import no.nav.dagpenger.mediator.objectMapper
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.BehandlingObservatør
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

@Execution(ExecutionMode.SAME_THREAD)
internal class OppdateringRepositoryFlytPostgresTest {
    @Test
    fun `observer lagrer oppdatering som kan leses via sse-kilde`() {
        withMigrertIsolertDb {
            val repository = OppdateringRepositoryPostgres(dbSession)
            val observer = OppdateringObserver()
            val ident = "12345678910"
            val behandlingId = UUID.randomUUID()

            observer.endretTilstand(
                BehandlingObservatør
                    .BehandlingEndretTilstand(
                        behandlingId = behandlingId,
                        gjeldendeTilstand = Behandling.TilstandType.TilGodkjenning,
                        forrigeTilstand = Behandling.TilstandType.UnderBehandling,
                        forventetFerdig = LocalDateTime.now().plusMinutes(1),
                        tidBrukt = Duration.ofSeconds(5),
                    ).also { it.ident = ident },
            )
            observer.ferdigstill(repository, UUID.randomUUID())

            val kilde = repository.oppdateringerForBehandling(behandlingId)
            val innslag = kilde.hentInnslag(0)

            innslag.shouldHaveSize(1)
            innslag.single().event shouldBe "behandling_endret_tilstand"
            objectMapper.readTree(innslag.single().data) shouldBe
                objectMapper.readTree(
                    """
                    {"ident":"$ident","behandlingId":"$behandlingId","forrigeTilstand":"UnderBehandling","gjeldendeTilstand":"TilGodkjenning"}
                    """.trimIndent(),
                )
            kilde.defaultCursor() shouldBe innslag.single().id
        }
    }

    @Test
    fun `lagring er idempotent for samme hendelse, type og payloadHash`() {
        withMigrertIsolertDb {
            val repository = OppdateringRepositoryPostgres(dbSession)
            val ident = "12345678910"
            val behandlingId = UUID.randomUUID()
            val hendelseId = UUID.randomUUID()
            val payload = """{"ident":"$ident","behandlingId":"$behandlingId"}"""

            val oppdatering =
                NyOppdatering(
                    hendelseId = hendelseId,
                    ident = ident,
                    behandlingId = behandlingId,
                    type = "behandling_opprettet",
                    payload = payload,
                    payloadHash = payload.sha256(),
                )

            repository.lagre(listOf(oppdatering))
            repository.lagre(listOf(oppdatering))

            repository.hentForBehandling(behandlingId, 0).shouldHaveSize(1)
        }
    }

    private fun String.sha256(): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }

    private inline fun withMigrertIsolertDb(crossinline block: no.nav.dagpenger.mediator.db.DBTestContext.() -> Unit) {
        Thread.sleep(5)
        withIsolatedDb {
            runMigration()
            block()
        }
    }
}
