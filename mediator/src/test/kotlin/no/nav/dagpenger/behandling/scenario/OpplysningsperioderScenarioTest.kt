package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.Opphold
import org.junit.jupiter.api.Test
import java.util.UUID

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
                perioder[0].gyldigFraOgMed shouldBe null
                perioder[0].gyldigTilOgMed shouldBe null
                perioder[0].verdi.verdi shouldBe true
            }

            // Endring av verdi med samme gyldighetsperiode
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, false, "fjas", Gyldighetsperiode())

            // Assert at vi har endret verdi
            behandlingsresultatForslag {
                val perioder = opplysninger(Opphold.oppholdINorge)
                perioder shouldHaveSize 1
                perioder[0].gyldigFraOgMed shouldBe null
                perioder[0].gyldigTilOgMed shouldBe null
                perioder[0].verdi.verdi shouldBe false // Endret til false
            }
        }
    }

    @Test
    fun `kan ikke legge til opplysninger med overlappende perioder`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            var opprinneligId: UUID? = null
            behandlingsresultatForslag {
                val perioder = opplysninger(Opphold.oppholdINorge)
                perioder[0].gyldigFraOgMed shouldBe null
                perioder[0].gyldigTilOgMed shouldBe null
                perioder[0].verdi.verdi shouldBe true

                opprinneligId = perioder[0].id
            }

            // Dette skal ikke være mulig
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, false, "fjas", Gyldighetsperiode(26.juni(2018)))

            // Assert at vi ikke har lagt til noen nye perioder
            behandlingsresultatForslag {
                val perioder = opplysninger(Opphold.oppholdINorge)
                perioder shouldHaveSize 1
                perioder[0].gyldigFraOgMed shouldBe null
                perioder[0].gyldigTilOgMed shouldBe null
                perioder[0].verdi.verdi shouldBe true // Fortsatt true

                // TODO: Vi må bestemme om vi vil ha en id som er den samme
                // perioder[0].id shouldBe opprinneligId
            }

            // Opprinnelig periode må først kuttes
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, true, "fjas", Gyldighetsperiode(tilOgMed = 25.juni(2018)))
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, false, "fjas", Gyldighetsperiode(fom = 26.juni(2018)))

            // Assert at vi ikke har lagt til noen nye perioder
            behandlingsresultatForslag {
                val perioder = opplysninger(Opphold.oppholdINorge)
                perioder shouldHaveSize 2
                perioder[0].gyldigFraOgMed shouldBe null
                perioder[0].gyldigTilOgMed shouldBe 25.juni(2018)

                perioder[1].gyldigFraOgMed shouldBe 26.juni(2018)
                perioder[1].gyldigTilOgMed shouldBe null
            }
        }
    }
}
