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
            }
        }
    }

    @Test
    fun `godkjent etablering setter sluttdato på løpende rett`() {
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
                opplysninger(etableringGodkjent) {
                    single().verdi.verdi shouldBe false
                }

                with(rettighetsperioder.single()) {
                    harRett shouldBe true
                    fraOgMed shouldBe 1.januar(2025)
                    tilOgMed shouldBe null
                }
            }

            saksbehandler.endreOpplysning(Etablering.sluttDato, 31.januar(2025))
            listOf(
                Etablering.nyVirksomhet,
                Etablering.selvforsørget,
                Etablering.godkjentNæringsfaglig,
                Etablering.egenVirksomhet,
                Etablering.ikkeSelvforskyldtArbeidsledig,
                Etablering.overFemtiProsentEierandel,
            ).forEach { saksbehandler.endreOpplysning(it, true) }

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(2) {
                opplysninger(Rettighetstype.skalEtableringVurderes) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe true
                }
                opplysninger(etableringGodkjent) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe true
                    single().gyldigTilOgMed shouldBe 31.januar(2025)
                }

                with(rettighetsperioder.single()) {
                    harRett shouldBe true
                    fraOgMed shouldBe 1.januar(2025)
                    tilOgMed shouldBe 31.januar(2025)
                }
            }
        }
    }
}
