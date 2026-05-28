package no.nav.dagpenger.mediator.api

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.mediator.api.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.mediator.august
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold.oppholdINorge
import no.nav.dagpenger.scenario.SimulertDagpengerSystem
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Test
import tools.jackson.core.type.TypeReference
import tools.jackson.module.kotlin.jacksonObjectMapper

internal class TidslinjeApiTest {
    @Test
    fun `henter tidslinje for person med søknad, stans, gjenopptak og meldekort`() {
        medSikretBehandlingApi {
            // 1. Søknad → Innvilgelse
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // 2. Meldekort
            person.sendInnMeldekort(1)
            meldekortBatch(true)

            // 3. Stans
            person.opprettBehandling(22.juli(2018))
            saksbehandler.endreOpplysning(oppholdINorge, false, "Er i utlandet", Gyldighetsperiode(22.juli(2018)))
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // 4. Gjenopptak
            person.søkGjenopptak(23.august(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(oppholdINorge, true, "Tilbake fra utlandet", Gyldighetsperiode(23.august(2018)))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Hent tidslinje
            val response =
                it.autentisert(
                    endepunkt = "/person/tidslinje",
                    body = """{"ident":"${person.ident}"}""",
                )

            response.status shouldBe HttpStatusCode.OK
            val body = response.bodyAsText()
            val tidslinje =
                objectMapper.readValue(body, object : TypeReference<Map<String, Any?>>() {})

            val behandlinger = tidslinje["behandlinger"] as List<*>
            val rettighetsperioder = tidslinje["rettighetsperioder"] as List<*>

            behandlinger.shouldHaveSize(4) // Søknad + meldekort + stans + gjenopptak
            rettighetsperioder.shouldHaveSize(3) // Innvilget + stanset + gjenopptatt

            // Skriv til fil for inspeksjon
            val output = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tidslinje)
            java.io.File("build/tidslinje-output.json").writeText(output)
            println(output)
        }
    }

    private fun medSikretBehandlingApi(block: suspend SimulertDagpengerSystem.(TestContext) -> Unit) {
        nyttScenario {
            inntektSiste12Mnd = 500000
            saksbehandlerGruppe = "dagpenger-saksbehandler"
        }.test {
            withMockAuthServerAndTestApplication(this.api) { block(this) }
        }
    }

    private companion object {
        private val objectMapper = jacksonObjectMapper()
    }
}
