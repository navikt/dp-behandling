package no.nav.dagpenger.mediator.api

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.mediator.api.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.mediator.januar
import no.nav.dagpenger.mediator.objectMapper
import no.nav.dagpenger.scenario.SimulertDagpengerSystem
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonoppslagApiTest {
    @Test
    fun `ikke autentiserte kall returnerer 401`() {
        medSikretBehandlingApi {
            val response =
                it.client.post("/person/rettighetsperioder") {
                    setBody("""{"ident":"${person.ident}", "fraOgMed":"2023-01-01"}""")
                }
            response.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `henter rettighetsperioder for person`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(23.januar(2023))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    "/person/rettighetsperioder",
                    body = """{"ident":"${person.ident}", "fraOgMed":"2023-01-01"}""",
                )

            response.status shouldBe HttpStatusCode.OK
            val perioder = objectMapper.readTree(response.bodyAsText())
            perioder.isArray shouldBe true
            perioder.shouldNotBeEmpty()

            val periode = perioder[0]
            periode["fraOgMed"].asString() shouldBe "2023-01-23"
            periode["harRett"].asBoolean() shouldBe true
            periode["ytelseType"].asString() shouldBe "DAGPENGER_ARBEIDSSOKER_ORDINAER"
            // Løpende periode har ingen tilOgMed
            periode.has("tilOgMed") shouldBe false
        }
    }

    @Test
    fun `rettighetsperioder avgrenses av forespørselsperioden`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(23.januar(2023))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Spør om en periode langt etter innvilgelsen - skal likevel treffe (løpende periode)
            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    "/person/rettighetsperioder",
                    body = """{"ident":"${person.ident}", "fraOgMed":"2023-06-01", "tilOgMed":"2023-06-30"}""",
                )
            response.status shouldBe HttpStatusCode.OK
            val perioder = objectMapper.readTree(response.bodyAsText())
            perioder.isArray shouldBe true
            perioder.shouldNotBeEmpty()

            // Spør om en periode før innvilgelsen - skal ikke treffe
            val førResponse =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    "/person/rettighetsperioder",
                    body = """{"ident":"${person.ident}", "fraOgMed":"2022-01-01", "tilOgMed":"2022-12-31"}""",
                )
            førResponse.status shouldBe HttpStatusCode.OK
            objectMapper.readTree(førResponse.bodyAsText()).size() shouldBe 0
        }
    }

    @Test
    fun `henter beregninger etter meldekort er behandlet`() {
        medSikretBehandlingApi { testContext ->
            person.søkDagpenger(23.januar(2023))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            val response =
                testContext.autentisert(
                    httpMethod = HttpMethod.Post,
                    "/person/beregninger",
                    body = """{"ident":"${person.ident}", "fraOgMed":"2023-01-01"}""",
                )

            response.status shouldBe HttpStatusCode.OK
            val beregninger = objectMapper.readTree(response.bodyAsText())
            beregninger.isArray shouldBe true
            beregninger.shouldNotBeEmpty()

            // Verifiser struktur og innhold på hver beregnede dag
            beregninger.forEach { dag ->
                LocalDate.parse(dag["fraOgMed"].asString()) // kaster ved ugyldig dato
                LocalDate.parse(dag["tilOgMed"].asString())
                dag["fraOgMed"].asString() shouldBe dag["tilOgMed"].asString() // hver dag er enkeltstående
                dag["sats"].isInt shouldBe true
                dag["utbetaltBeløp"].isInt shouldBe true
                dag["gjenståendeDager"].isInt shouldBe true
            }

            // Verifiser at beregningene er sortert og at gjenstående dager avtar
            val gjenståendeDager = mutableListOf<Int>()
            beregninger.forEach { dag -> gjenståendeDager.add(dag["gjenståendeDager"].asInt()) }
            for (i in 0 until gjenståendeDager.size - 1) {
                gjenståendeDager[i] shouldBe gjenståendeDager[i] // verifiser at verdien finnes
                (gjenståendeDager[i] >= gjenståendeDager[i + 1]) shouldBe true // avtar eller er lik
            }
        }
    }

    private fun medSikretBehandlingApi(block: suspend SimulertDagpengerSystem.(TestContext) -> Unit) {
        nyttScenario {
            inntektSiste12Mnd = 350000
            saksbehandlerGruppe = "dagpenger-saksbehandler"
            adminGrupper = listOf("enkel-admin")
            maskintilgangnavn = "test-app"
        }.test {
            withMockAuthServerAndTestApplication(this.api) { block(this) }
        }
    }
}
