package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.februar
import no.nav.dagpenger.mediator.januar
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import kotlin.test.Test

class EksportTest {
    @Test
    fun `eksport av dagpenger til EØS-land`() {
        nyttScenario {
            inntektSiste12Mnd = 5000000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().fraOgMed shouldBe 1.januar(2025)
                rettighetsperioder.single().tilOgMed shouldBe null
                opplysninger(Rettighetstype.skalEksportVurderes) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }
                opplysninger(Eksport.oppyllerVilkårForEksport) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(2) {
                førteTil shouldBe "Endring"
            }

            saksbehandler.lagBehandling(1.februar(2025))
            saksbehandler.endreOpplysning(
                Rettighetstype.skalEksportVurderes,
                true,
                "Drar til syden",
                gyldighetsperiode = Gyldighetsperiode(1.februar(2025)),
            )

            behandlingsresultatForslag {
                opplysninger(Rettighetstype.skalEksportVurderes) {
                    shouldHaveSize(2)
                    this[0].verdi.verdi shouldBe false
                    this[0].gyldigTilOgMed shouldBe 31.januar(2025)
                    this[1].verdi.verdi shouldBe true
                    this[1].gyldigFraOgMed shouldBe 1.februar(2025)
                }
            }

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Eksport godkjent og bruker får stans fram til registrering i vertsland
            behandlingsresultat(3) {
                opplysninger(Rettighetstype.skalEksportVurderes) {
                    shouldHaveSize(2)
                    this[0].verdi.verdi shouldBe false
                    this[0].gyldigTilOgMed shouldBe 31.januar(2025)
                    this[1].verdi.verdi shouldBe true
                    this[1].gyldigFraOgMed shouldBe 1.februar(2025)
                }
                opplysninger(Eksport.oppyllerVilkårForEksport) {
                    shouldHaveSize(1)
                    this[0].verdi.verdi shouldBe false
                    this[0].gyldigFraOgMed shouldBe 1.februar(2025)
                }
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe false
            }

            // Registrerer seg i vertslandet og skal ha gjenopptak av dagpenger
            saksbehandler.lagBehandling(5.februar(2025))
            saksbehandler.endreOpplysning(
                Eksport.registrertIVertsland,
                true,
                "Har registrert seg i syden",
                gyldighetsperiode = Gyldighetsperiode(5.februar(2025)),
            )

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Registert i vertslandet og dagpenger begynner å løpe
            behandlingsresultat(4) {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe true
                rettighetsperioder.last().fraOgMed shouldBe 1.februar(2025)
            }

            // TODO: Se på samspill med stans etter 21 dager
        }
    }

    @Test
    fun `eksport av dagpenger til EØS-land hvor bruker registres etter fristen`() {
        nyttScenario {
            inntektSiste12Mnd = 5000000
        }.test {
            person.søkDagpenger(1.januar(2025))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            saksbehandler.lagBehandling(1.februar(2025))
            saksbehandler.endreOpplysning(
                Rettighetstype.skalEksportVurderes,
                true,
                "Drar til syden",
                gyldighetsperiode = Gyldighetsperiode(1.februar(2025)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Eksport godkjent og bruker får stans fram til registrering i vertsland
            behandlingsresultat(3) {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe false
            }

            // Registrerer seg i vertslandet og skal ha gjenopptak av dagpenger
            saksbehandler.lagBehandling(5.februar(2025))
            saksbehandler.endreOpplysning(
                Eksport.registrertIVertsland,
                true,
                "Har registrert seg i syden",
                gyldighetsperiode = Gyldighetsperiode(15.februar(2025)),
            )

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Registert i vertslandet og dagpenger begynner å løpe
            behandlingsresultat(4) {
                rettighetsperioder shouldHaveSize 3
                rettighetsperioder.last().harRett shouldBe true
                rettighetsperioder.last().fraOgMed shouldBe 15.februar(2025)
            }

            // TODO: Se på samspill med stans etter 21 dager
        }
    }
}
