package no.nav.dagpenger.behandling.mediator.api

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.behandling.mediator.api.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.behandling.simulering.api.models.BeregningDTO
import org.junit.jupiter.api.Test

internal class SimuleringApiTest {
    @Test
    fun `simulerer beregning av meldekort`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { this.simuleringApi() },
            {
                val response =
                    this.client.put("simulering/beregning") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        setBody(json)
                    }

                response.status shouldBe HttpStatusCode.OK
                val beregningDTO = objectMapper.readValue<BeregningDTO>(response.bodyAsText())
                beregningDTO.dager.size shouldBe 10
            },
        )
    }

    // language=JSON
    private val json = """
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
                              "dagsats": [
                                {
                                  "verdi": 100.0
                                }
                              ],
                              "fastsattVanligArbeidstid": 40.0,
                              "egenandel": 300.0,
                              "dager": [
                                {
                                  "dato": "2020-01-16",
                                  "aktiviteter": [
                                    {
                                      "type": "Arbeid",
                                      "timer": "PT8H30M"
                                    }
                                  ],
                                  "dagIndex": 0,
                                  "meldt": true
                                },
                                 {
                                  "dato": "2020-01-17",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                 {
                                  "dato": "2020-01-18",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                 {
                                  "dato": "2020-01-19",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                 {
                                  "dato": "2020-01-20",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                   {
                                  "dato": "2020-01-21",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                   {
                                  "dato": "2020-01-22",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                   {
                                  "dato": "2020-01-23",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                   {
                                  "dato": "2020-01-24",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                   {
                                  "dato": "2020-01-25",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },   {
                                  "dato": "2020-01-26",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                   {
                                  "dato": "2020-01-27",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                   {
                                  "dato": "2020-01-28",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                },
                                   {
                                  "dato": "2020-01-29",
                                  "aktiviteter": [ ],
                                  "dagIndex": 1,
                                  "meldt": true
                                }
                              ]
                            }
                            """

    private companion object {
        private val objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}
