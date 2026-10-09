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
    fun `etablering påvirker ikke resultatet der en har ikke godkjent etablering`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()

            val etableringFra = 10.januar(2025)
            saksbehandler.endreOpplysning(
                Rettighetstype.skalEtableringVurderes,
                false,
                "Har startet egen virksomhet",
                gyldighetsperiode = Gyldighetsperiode(fraOgMed = 1.januar(2025), tilOgMed = etableringFra.minusDays(1)),
            )
            saksbehandler.endreOpplysning(
                Rettighetstype.skalEtableringVurderes,
                true,
                "Har startet egen virksomhet",
                gyldighetsperiode = Gyldighetsperiode(etableringFra),
            )

            listOf(
                Etablering.nyVirksomhet,
                Etablering.selvforsørget,
                Etablering.godkjentNæringsfaglig,
                Etablering.ikkeSelvforskyldtArbeidsledig,
                Etablering.overFemtiProsentEierandel,
            ).forEach { saksbehandler.endreOpplysning(it, false, gyldighetsperiode = Gyldighetsperiode(fraOgMed = etableringFra)) }

            saksbehandler.endreOpplysning(Etablering.sluttDato, 31.januar(2025))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().fraOgMed shouldBe 1.januar(2025)
                rettighetsperioder.single().tilOgMed shouldBe null

                opplysninger(Rettighetstype.skalEtableringVurderes) {
                    shouldHaveSize(2)
                    first().verdi.verdi shouldBe false
                    last().verdi.verdi shouldBe true
                }
                opplysninger(etableringGodkjent) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }
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
            val etableringFra = 10.januar(2025)
            saksbehandler.endreOpplysning(
                Rettighetstype.skalEtableringVurderes,
                true,
                "Har startet egen virksomhet",
                gyldighetsperiode = Gyldighetsperiode(etableringFra),
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
                Etablering.ikkeSelvforskyldtArbeidsledig,
                Etablering.overFemtiProsentEierandel,
            ).forEach { saksbehandler.endreOpplysning(it, true, gyldighetsperiode = Gyldighetsperiode(fraOgMed = etableringFra)) }

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(2) {
                with(rettighetsperioder) {
                    size shouldBe 2
                    first().harRett shouldBe true
                    first().fraOgMed shouldBe 1.januar(2025)
                    first().tilOgMed shouldBe 9.januar(2025)
                    last().harRett shouldBe true
                    last().fraOgMed shouldBe 10.januar(2025)
                    last().tilOgMed shouldBe 31.januar(2025)
                }
                opplysninger(Rettighetstype.skalEtableringVurderes) {
                    shouldHaveSize(2)
                    first().verdi.verdi shouldBe false
                    last().verdi.verdi shouldBe true
                }
                opplysninger(etableringGodkjent) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe true
                    single().gyldigFraOgMed shouldBe 10.januar(2025)
                    single().gyldigTilOgMed shouldBe 31.januar(2025)
                }
            }
        }
    }
}
