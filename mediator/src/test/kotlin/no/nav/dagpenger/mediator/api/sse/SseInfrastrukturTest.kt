package no.nav.dagpenger.mediator.api.sse

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import no.nav.dagpenger.mediator.api.oppdateringerForPerson
import no.nav.dagpenger.mediator.api.statusPagesConfig
import no.nav.dagpenger.mediator.objectMapper
import no.nav.dagpenger.mediator.repository.NyOppdatering
import no.nav.dagpenger.mediator.repository.OppdateringInnslag
import no.nav.dagpenger.mediator.repository.OppdateringRepository
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

internal class SseInfrastrukturTest {
    @Test
    fun `bruker default cursor når ingen cursor er oppgitt`() =
        testApplication {
            application {
                routing {
                    get("/cursor") {
                        call.respondText(call.sseCursorEller { 77L }.toString())
                    }
                }
            }

            val response = client.get("/cursor")

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe "77"
        }

    @Test
    fun `bruker Last-Event-ID foran since`() =
        testApplication {
            application {
                routing {
                    get("/cursor") {
                        call.respondText(call.sseCursorEller { 0L }.toString())
                    }
                }
            }

            val response =
                client.get("/cursor?since=11") {
                    header("Last-Event-ID", "12")
                }

            response.status shouldBe HttpStatusCode.OK
            response.bodyAsText() shouldBe "12"
        }

    @Test
    fun `ugyldig cursor gir 400`() =
        testApplication {
            application {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(objectMapper))
                }
                install(StatusPages) {
                    statusPagesConfig()
                }
                routing {
                    get("/cursor") {
                        call.respondText(call.sseCursorEller { 0L }.toString())
                    }
                }
            }

            val response = client.get("/cursor?since=abc")

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText() shouldContain "Ugyldig cursor i since/Last-Event-ID"
        }

    @Test
    fun `avviser nye streams når sse-slotter er fulle`() =
        testApplication {
            val frigjør = CompletableDeferred<Unit>()
            val aktiveStreams = AtomicInteger(0)
            application {
                routing {
                    get("/hold") {
                        call.medSseStreamSlot {
                            aktiveStreams.incrementAndGet()
                            frigjør.await()
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
            }

            coroutineScope {
                val pågående = (1..100).map { async { client.get("/hold") } }
                withTimeout(5_000.milliseconds) {
                    while (aktiveStreams.get() < 100) {
                        delay(10.milliseconds)
                    }
                }

                val avvist = client.get("/hold")
                avvist.status shouldBe HttpStatusCode.TooManyRequests

                frigjør.complete(Unit)
                pågående.awaitAll().forEach { it.status shouldBe HttpStatusCode.OK }
            }
        }

    @Test
    fun `kilde for person mapper repository-innslag til sse-innslag`() {
        val forventetIdent = "12345678910"
        val repository =
            FakeOppdateringRepository(
                personInnslag = listOf(OppdateringInnslag(id = 10, type = "behandling_endret", payload = """{"x":1}""")),
                sisteIdPerson = 9,
            )

        val kilde = repository.oppdateringerForPerson(forventetIdent)
        val innslag = kilde.hentInnslag(9)

        kilde.defaultCursor() shouldBe 9
        repository.sistEtterIdForPerson shouldBe 9
        repository.sistIdent shouldBe forventetIdent
        innslag shouldBe listOf(SseInnslag(id = 10, event = "behandling_endret", data = """{"x":1}"""))
    }

    private class FakeOppdateringRepository(
        private val personInnslag: List<OppdateringInnslag> = emptyList(),
        private val behandlingInnslag: List<OppdateringInnslag> = emptyList(),
        private val sisteIdPerson: Long = 0,
        private val sisteIdBehandling: Long = 0,
    ) : OppdateringRepository {
        var sistEtterIdForPerson: Long? = null
        var sistIdent: String? = null

        override fun lagre(oppdateringer: List<NyOppdatering>) = Unit

        override fun sisteIdForIdent(ident: String): Long = sisteIdPerson.also { sistIdent = ident }

        override fun sisteIdForBehandling(behandlingId: UUID): Long = sisteIdBehandling

        override fun hentForIdent(
            ident: String,
            etterId: Long,
            limit: Int,
        ): List<OppdateringInnslag> =
            personInnslag.also {
                sistIdent = ident
                sistEtterIdForPerson = etterId
            }

        override fun hentForBehandling(
            behandlingId: UUID,
            etterId: Long,
            limit: Int,
        ): List<OppdateringInnslag> = behandlingInnslag
    }
}
