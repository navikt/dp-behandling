package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem
import no.nav.dagpenger.behandling.helpers.scenario.assertions.Opplysningsperiode
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.beregning.Beregning
import org.junit.jupiter.api.Test

class BeregningTest {
    @Test
    fun `vi kan beregne meldekort og endre rettighetsperiode i samme behandling`() {
        SimulertDagpengerSystem.Companion
            .nyttScenario {
                inntektSiste12Mnd = 500000
            }.test {
                person.søkDagpenger(21.juni(2018))

                behovsløsere.løsTilForslag()
                saksbehandler.lukkAlleAvklaringer()
                saksbehandler.godkjenn()
                saksbehandler.beslutt()

                vedtak { utfall shouldBe true }

                // Send inn meldekort
                person.sendInnMeldekort(1)
                person.sendInnMeldekort(2)

                // Systemet kjører beregningsbatchen
                meldekortBatch(true)

                behandlingsresultatForslag {
                    with(opplysninger(Beregning.forbruk)) {
                        this shouldHaveSize 14

                        // Første dag i meldekort
                        this.first().gyldigFraOgMed shouldBe 18.juni(2018)

                        // Første forbruksdag
                        this.first { it.verdi.verdi == "true" }.gyldigFraOgMed shouldBe 21.juni(2018)

                        // Siste dag i meldekort
                        this.last().gyldigFraOgMed shouldBe 1.juli(2018)
                    }
                }

                // Systemet kjører beregningsbatchen
                meldekortBatch()

                // Vi lager et forslag om beregning for hele meldeperioden
                behandlingsresultatForslag {
                    with(opplysninger(Beregning.forbruk)) {
                        this shouldHaveSize 28

                        // Første dag i ny meldeperiode
                        this.first { it.status != Opplysningsperiode.Periodestatus.Arvet }.gyldigFraOgMed shouldBe 2.juli(2018)

                        // Siste dag i ny meldeperiode
                        this.last().gyldigFraOgMed shouldBe 15.juli(2018)

                        // Siste dag med forbruk
                        this.last { it.verdi.verdi == "true" }.gyldigFraOgMed shouldBe 13.juli(2018)
                    }
                }

                // Vilkår blir vurderte som ikke oppfylt
                saksbehandler.endreOpplysning(Opphold.oppholdINorge, false, "", Gyldighetsperiode(8.juli(2018)))

                // Behandlingen av meldekort har stoppet opp og vi endrer rettighetsperiode på grunn av stans
                saksbehandler.endreOpplysning(harLøpendeRett, false, "", Gyldighetsperiode(8.juli(2018)))

                // Verifiser at behandlingen nå bare beregner et subset av meldeperioden
                behandlingsresultatForslag {
                    with(opplysninger(harLøpendeRett)) {
                        this shouldHaveSize 2
                        this.first().gyldigFraOgMed shouldBe 21.juni(2018)
                        this.last().gyldigFraOgMed shouldBe 8.juli(2018)
                    }

                    with(opplysninger(Beregning.forbruk)) {
                        this shouldHaveSize 28

                        // Første dag i ny meldeperiode
                        this.first { it.status != Opplysningsperiode.Periodestatus.Arvet }.gyldigFraOgMed shouldBe 2.juli(2018)

                        // Siste dag i ny meldeperiode
                        this.last().gyldigFraOgMed shouldBe 15.juli(2018)

                        // Siste dag med forbruk (8. juli er søndag, så siste forbruksdag blir fredag 6. juli)
                        this.last { it.verdi.verdi == "true" }.gyldigFraOgMed shouldBe 6.juli(2018)
                    }
                }
            }
    }
}
