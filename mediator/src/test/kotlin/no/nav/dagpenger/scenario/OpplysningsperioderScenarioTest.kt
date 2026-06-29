package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold.oppholdINorge
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.scenario.assertions.Opplysningsperiode.Periodestatus
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

    @Test
    fun `unngå at opplysninger fra somUtgangspunkt legger seg over eksisterende`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.endreOpplysning(
                Utdanning.deltakelseIArbeidsmarkedstiltak,
                false,
                "fjas",
                Gyldighetsperiode(21.juni(2018), 25.juni(2018)),
            )
            saksbehandler.endreOpplysning(
                Utdanning.deltakelseIArbeidsmarkedstiltak,
                true,
                "fjas",
                Gyldighetsperiode(26.juni(2018), 5.juli(2018)),
            )
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                val perioder = opplysninger(Utdanning.deltakelseIArbeidsmarkedstiltak)
                perioder[0].gyldigFraOgMed shouldBe 21.juni(2018)
                perioder[0].gyldigTilOgMed shouldBe 25.juni(2018)
                perioder[0].verdi.verdi shouldBe false

                perioder[1].gyldigFraOgMed shouldBe 26.juni(2018)
                perioder[1].gyldigTilOgMed shouldBe 5.juli(2018)
                perioder[1].verdi.verdi shouldBe true
            }

            saksbehandler.lagBehandling(15.juli(2018))

            // Assert at vi har endret verdi
            behandlingsresultatForslag {
                val perioder = opplysninger(Utdanning.deltakelseIArbeidsmarkedstiltak)
                perioder shouldHaveSize 3

                // Perioder arvet fra forrige behandling
                perioder[0].gyldigFraOgMed shouldBe 21.juni(2018)
                perioder[0].gyldigTilOgMed shouldBe 25.juni(2018)
                perioder[0].verdi.verdi shouldBe false
                perioder[0].opprinnelse shouldBe Periodestatus.Arvet

                perioder[1].gyldigFraOgMed shouldBe 26.juni(2018)
                perioder[1].gyldigTilOgMed shouldBe 5.juli(2018)
                perioder[1].verdi.verdi shouldBe true
                perioder[1].opprinnelse shouldBe Periodestatus.Arvet

                // Ny standardperiode fra ny behandlings regelverksdato
                perioder[2].gyldigFraOgMed shouldBe 15.juli(2018)
                perioder[2].gyldigTilOgMed shouldBe null
                perioder[2].verdi.verdi shouldBe false
                perioder[2].opprinnelse shouldBe Periodestatus.Ny
            }
        }
    }
}
