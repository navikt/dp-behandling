package no.nav.dagpenger.mediator.api

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.mediator.api.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.mediator.objectMapper
import no.nav.dagpenger.mediator.simulering.RegelsettRegister
import no.nav.dagpenger.regel.DagpengerRegistrering
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

@Suppress("UNCHECKED_CAST")
internal class GenerellSimuleringApiTest {
    private val register = RegelsettRegister(listOf(DagpengerRegistrering()))

    @Test
    fun `henter liste over alle regelverk`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { generellSimuleringApi(register) },
        ) {
            val response = client.get("simulering/regelverk")
            response.status shouldBe HttpStatusCode.OK

            val body = objectMapper.readValue<List<Map<String, Any>>>(response.bodyAsText())
            body.shouldNotBeEmpty()
            body.first() shouldContainKey "navn"
            body.first() shouldContainKey "href"
        }
    }

    @Test
    fun `henter graf for Dagpenger-regelverk`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { generellSimuleringApi(register) },
        ) {
            val response = client.get("simulering/regelverk/Dagpenger")
            response.status shouldBe HttpStatusCode.OK

            val body = objectMapper.readValue<Map<String, Any>>(response.bodyAsText())
            body["navn"] shouldBe "Dagpenger"
            val regelsett = body["regelsett"] as List<*>
            regelsett.shouldNotBeEmpty()
        }
    }

    @Test
    fun `returnerer 404 for ukjent regelverk`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { generellSimuleringApi(register) },
        ) {
            val response = client.get("simulering/regelverk/FinnesIkke")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `henter schema for Alderskrav-regelsett`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { generellSimuleringApi(register) },
        ) {
            val response = client.get("simulering/regelverk/Dagpenger/regelsett/Alder")
            response.status shouldBe HttpStatusCode.OK

            val body = objectMapper.readValue<Map<String, Any>>(response.bodyAsText())
            body["navn"] shouldBe "Alder"
            body["type"] shouldBe "Vilkår"
            val inndata = body["inndata"] as List<*>
            inndata.shouldNotBeEmpty()
            val produserer = body["produserer"] as List<*>
            produserer.shouldNotBeEmpty()
            body shouldContainKey "upstream"
            body shouldContainKey "downstream"
        }
    }

    @Test
    fun `returnerer 404 for ukjent regelsett`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { generellSimuleringApi(register) },
        ) {
            val response = client.get("simulering/regelverk/Dagpenger/regelsett/FinnesIkke")
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    @Test
    fun `evaluerer Alderskrav og returnerer evalueringstre`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { generellSimuleringApi(register) },
        ) {
            // language=JSON
            val request =
                """
                {
                  "dato": "2024-06-19",
                  "opplysninger": {
                    "Fødselsdato": "1980-01-01",
                    "Prøvingsdato": "2024-06-19"
                  }
                }
                """.trimIndent()

            val response =
                client.post("simulering/regelverk/Dagpenger/regelsett/Alder/evaluer") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(request)
                }

            response.status shouldBe HttpStatusCode.OK
            val body = objectMapper.readValue<Map<String, Any>>(response.bodyAsText())
            val opplysninger = body["opplysninger"] as List<*>
            opplysninger.shouldNotBeEmpty()
        }
    }

    @Test
    fun `evaluerer Alderskrav med manglende inndata og rapporterer mangler`() {
        withMockAuthServerAndTestApplication(
            moduleFunction = { generellSimuleringApi(register) },
        ) {
            // language=JSON
            val request =
                """
                {
                  "dato": "2024-06-19",
                  "opplysninger": {}
                }
                """.trimIndent()

            val response =
                client.post("simulering/regelverk/Dagpenger/regelsett/Alder/evaluer") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(request)
                }

            response.status shouldBe HttpStatusCode.OK
            val body = objectMapper.readValue<Map<String, Any>>(response.bodyAsText())
            val mangler = body["mangler"] as List<*>
            mangler.shouldNotBeEmpty()
        }
    }
}
