package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.behandling.september
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.Gjenopptak
import no.nav.dagpenger.regel.Opphold.oppholdINorge
import org.junit.jupiter.api.Test

class GjenopptakScenarioTest {
    @Test
    fun `Gjenopptak scenario`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Send inn meldekort
            person.sendInnMeldekort(1)
            meldekortBatch(true)
            person.sendInnMeldekort(2)
            meldekortBatch(true)

            behandlingsresultat {
                utbetalinger.size().shouldBeGreaterThan(10)
            }

            // Stanser dagpenger
            saksbehandler.lagBehandling(10.juli(2018))
            saksbehandler.endreOpplysning(oppholdINorge, false, "Er i utlandet", Gyldighetsperiode(10.juli(2018)))
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Bruker gjenopptar dagpenger
            person.søkGjenopptak(1.september(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(oppholdINorge, true, "Kommet hjem igjen", Gyldighetsperiode(1.september(2018)))
            saksbehandler.endreOpplysning(
                Gjenopptak.oppholdMedArbeidI12ukerEllerMer,
                true,
                "Skal reberegne grunnlag",
                Gyldighetsperiode(1.september(2018)),
            )
            val behov = rapidInspektør.message(rapidInspektør.size - 1)
            // Forventer at inntekt blir behov
            behov["@event_name"].asText() shouldBe "behov"
        }
    }
}
