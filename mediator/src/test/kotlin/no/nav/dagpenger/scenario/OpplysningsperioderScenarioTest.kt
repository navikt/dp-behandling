package no.nav.dagpenger.scenario

import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
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
                opplysning(Opphold.oppholdINorge) {
                    fra(21.juni(2018)) verdi true
                }
            }

            // Endring av verdi med samme gyldighetsperiode
            saksbehandler.leggTilOpplysning(Opphold.oppholdINorge, false, "fjas", Gyldighetsperiode(21.juni(2018)))

            // Assert at vi har endret verdi
            behandlingsresultatForslag {
                opplysning(Opphold.oppholdINorge) {
                    fra(21.juni(2018)) verdi false
                }
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

            saksbehandler.leggTilOpplysning(
                Utdanning.deltakelseIArbeidsmarkedstiltak,
                false,
                "fjas",
                Gyldighetsperiode(21.juni(2018), 25.juni(2018)),
            )
            saksbehandler.leggTilOpplysning(
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
                opplysning(Utdanning.deltakelseIArbeidsmarkedstiltak) {
                    fra(21.juni(2018)) til 25.juni(2018) verdi false
                    fra(26.juni(2018)) til 5.juli(2018) verdi true
                    fra(6.juli(2018)) verdi false
                }
            }

            saksbehandler.lagBehandling(15.juli(2018))

            // Assert at vi har endret verdi
            behandlingsresultatForslag {
                opplysning(Utdanning.deltakelseIArbeidsmarkedstiltak) {
                    fra(21.juni(2018)) til 25.juni(2018) verdi false opprinnelse Periodestatus.Arvet
                    fra(26.juni(2018)) til 5.juli(2018) verdi true opprinnelse Periodestatus.Arvet
                    fra(6.juli(2018)) verdi false opprinnelse Periodestatus.Arvet
                }
            }
        }
    }
}
