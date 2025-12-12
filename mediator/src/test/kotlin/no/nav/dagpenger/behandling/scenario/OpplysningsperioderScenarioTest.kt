package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.Opphold
import org.junit.jupiter.api.Test

// Tester ulike scenarier for kjeding av behandlinger
class OpplysningsperioderScenarioTest {
    @Test
    fun `kan endre verdi av eksisterende opplysning med uendelig gyldighetsperiode`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                val perioder = opplysninger(Opphold.oppholdINorge)
                perioder[0].gyldigFraOgMed shouldBe 21.juni(2018)
                perioder[0].gyldigTilOgMed shouldBe null
                perioder[0].verdi.verdi shouldBe true
            }

            // Endring av verdi med samme gyldighetsperiode
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, false, "fjas", Gyldighetsperiode(21.juni(2018)))

            // Assert at vi har endret verdi
            behandlingsresultatForslag {
                val perioder = opplysninger(Opphold.oppholdINorge)
                perioder shouldHaveSize 1
                perioder[0].gyldigFraOgMed shouldBe 21.juni(2018)
                perioder[0].gyldigTilOgMed shouldBe null
                perioder[0].verdi.verdi shouldBe false // Endret til false
            }
        }
    }
}
