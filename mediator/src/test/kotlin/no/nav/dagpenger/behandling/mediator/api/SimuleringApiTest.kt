package no.nav.dagpenger.behandling.mediator.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.behandling.mediator.api.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.Test
import kotlin.collections.forEach

internal class SimuleringApiTest {
    @Test
    fun `beregning uten meldekort`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { this.simuleringApi() },
            {
                val response =
                    this.client.put("simulering/beregning") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        setBody(
                            """
                               {
                              "terskel": [
                                {
                                  "verdi": 50.0
                                }
                              ],
                              "stonadsperiode": {
                                "fom": "2020-01-16",
                                "uker": 52
                              },
                              "meldekortFom": "2020-01-16",
                              "antallMeldekortdager": 14,
                              "sats": [
                                {
                                  "verdi": 100.0
                                }
                              ],
                              "fva": 40.0,
                              "egenandel": 300.0,
                              "meldekort": [
                                {
                                  "dag": "Mandag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Tirsdag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Onsdag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Torsdag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Fredag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Lørdag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Søndag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Mandag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Tirsdag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Onsdag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Torsdag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Fredag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Lørdag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                },
                                {
                                  "dag": "Søndag",
                                  "type": "Arbeidstimer",
                                  "verdi": 0.0
                                }
                              ]
                            }
                            """.trimIndent(),
                        )
                    }

                response.status shouldBe HttpStatusCode.OK

                val resp = response.bodyAsText()
                val svar = objectMapper.readTree(resp)
                println("""gjenståendeEgenandel: ${svar.get("gjenståendeEgenandel")}""")
                val dager = svar.get("dager")
                dager.forEach {
                    println(
                        """Dato: ${it.get("dato").asText()} """ +
                            """Sats: ${it.get("sats").asDouble()} """ +
                            """FVA: ${it.get("fva").asDouble()} """ +
                            """TimerArbeidet: ${it.get("timerArbeidet").asDouble()}""".trimIndent(),
                    )
                }
            },
        )
    }

    private companion object {
        private val objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}
