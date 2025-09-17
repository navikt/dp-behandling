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
import java.time.LocalDate

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
                beregningDTO.forbruktKvote shouldBe 10
                beregningDTO.dager.size shouldBe 14
                beregningDTO.dager.first().dato shouldBe LocalDate.of(2020, 1, 16)
                beregningDTO.dager.first().timerArbeidet shouldBe 8.5
            },
        )
    }

    @Test
    fun `simulerer beregning av meldekort 2`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { this.simuleringApi() },
            {
                val response =
                    this.client.put("simulering/beregning") {
                        header(HttpHeaders.ContentType, ContentType.Application.Json)
                        setBody(json2)
                    }

                response.status shouldBe HttpStatusCode.OK
            },
        )
    }

    // language=JSON
    private val json = """{
  "terskel": [
    {
      "verdi": 50.0
    }
  ],
  "stønadsperiode": {
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
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-18",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-19",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-20",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-21",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-22",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-23",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-24",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-25",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-26",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-27",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-28",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    },
    {
      "dato": "2020-01-29",
      "aktiviteter": [],
      "dagIndex": 1,
      "meldt": true
    }
  ]
}
                            """

    // language=JSON
    private val json2 =
        """
        {
          "antallMeldekortdager": 14,
          "dager": [
            {
              "dagIndex": 0,
              "dato": "2025-09-15",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 1,
              "dato": "2025-09-16",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 2,
              "dato": "2025-09-17",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 3,
              "dato": "2025-09-18",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 4,
              "dato": "2025-09-19",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 5,
              "dato": "2025-09-20",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 6,
              "dato": "2025-09-21",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 7,
              "dato": "2025-09-22",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 8,
              "dato": "2025-09-23",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 9,
              "dato": "2025-09-24",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 10,
              "dato": "2025-09-25",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 11,
              "dato": "2025-09-26",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 12,
              "dato": "2025-09-27",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 13,
              "dato": "2025-09-28",
              "aktiviteter": [],
              "meldt": true
            }
          ],
          "dagsats": [
            {
              "verdi": 800
            }
          ],
          "egenandel": 2400,
          "fastsattVanligArbeidstid": 40,
          "meldekortFom": "2025-09-17",
          "stønadsperiode": {
            "fom": "2025-09-01",
            "uker": 52
          },
          "terskel": [
            {
              "verdi": 40
            }
          ]
        }
        """.trimIndent()

    private companion object {
        private val objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
}
