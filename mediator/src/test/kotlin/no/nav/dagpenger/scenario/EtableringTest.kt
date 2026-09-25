package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.januar
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering.etableringGodkjent
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import kotlin.test.Test

class EtableringTest {
    @Test
    fun `etablering påvirker ikke resultatet før den skal vurderes`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder.single().harRett shouldBe true

                opplysninger(Rettighetstype.skalEtableringVurderes) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }

                // Regelsettet har ikke skalKjøres = true ennå, så ingen av opplysningene er produsert
                opplysninger(Etablering.nyVirksomhet).shouldBeEmpty()
                opplysninger(Etablering.påvirkerUtfallet).shouldBeEmpty()
            }
        }
    }

    @Test
    fun `etablering skal vurderes og påvirker resultatet med standardverdi`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            saksbehandler.omgjørBehandling(1.januar(2025))
            saksbehandler.endreOpplysning(
                Rettighetstype.skalEtableringVurderes,
                true,
                "Har startet egen virksomhet",
                gyldighetsperiode = Gyldighetsperiode(1.januar(2025)),
            )

            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                // Regelsettet kjøres nå, og standardverdiene (somUtgangspunkt) er produsert
                opplysninger(Etablering.nyVirksomhet) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }
                opplysninger(Etablering.selvforsørget) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }
                opplysninger(Etablering.godkjentNæringsfaglig) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }
                opplysninger(Etablering.ikkeSelvforskyldtArbeidsledig) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }

                // påvirkerUtfallet har somUtgangspunkt(true), så den er allerede sann uten at
                // saksbehandler trenger å overstyre den manuelt. Dette er det som gjør at
                // relevantForResultat (og dermed vilkåret i vilkårslisten) blir true.
                opplysninger(Etablering.påvirkerUtfallet) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe true
                }
            }

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(2) {
                opplysninger(Rettighetstype.skalEtableringVurderes) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe true
                }
                opplysninger(Etablering.påvirkerUtfallet) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe true
                }
                opplysninger(etableringGodkjent) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }

                rettighetsperioder.last().harRett shouldBe false
            }
        }
    }

    @Test
    fun `saksbehandler kan overstyre påvirkerUtfallet til false selv om standardverdien er true`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            saksbehandler.lagBehandling(1.januar(2025))
            saksbehandler.endreOpplysning(Rettighetstype.skalEtableringVurderes, true, "Har startet egen virksomhet")
            saksbehandler.endreOpplysning(Etablering.påvirkerUtfallet, false, "Etablering påvirker likevel ikke resultatet")

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(2) {
                opplysninger(Etablering.påvirkerUtfallet) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }

                rettighetsperioder.last().harRett shouldBe true
            }
        }
    }
}
