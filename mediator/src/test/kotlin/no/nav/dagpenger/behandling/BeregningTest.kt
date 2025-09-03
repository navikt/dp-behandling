package no.nav.dagpenger.behandling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.konfigurasjon.Feature
import no.nav.dagpenger.behandling.konfigurasjon.skruAvFeatures
import no.nav.dagpenger.behandling.konfigurasjon.skruPåFeature
import no.nav.dagpenger.behandling.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.scenario.assertions.Opplysningsperiode.Periodestatus.Arvet
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.beregning.Beregning
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BeregningTest {
    @BeforeEach
    fun setup() {
        // Forutsetter at unleash er skrudd på for kjeding av behandlinger
        skruPåFeature(Feature.KJEDING_AV_BEHANDLING)
    }

    @AfterEach
    fun tearDown() {
        // Nullstill unleash for å unngå at kjeding påvirker andre tester
        skruAvFeatures()
    }

    @Test
    fun `vi kan beregne meldekort og endre rettighetsperiode i samme behandling`() {
        nyttScenario {
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

            klumpen {
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
            klumpen {
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 28

                    // Første dag i ny meldeperiode
                    this.first { it.status != Arvet }.gyldigFraOgMed shouldBe 2.juli(2018)

                    // Siste dag i ny meldeperiode
                    this.last().gyldigFraOgMed shouldBe 15.juli(2018)

                    // Siste dag med forbruk
                    this.last { it.verdi.verdi == "true" }.gyldigFraOgMed shouldBe 13.juli(2018)
                }
            }

            // Behandlingen av meldekort har stoppet opp og vi endrer rettighetsperiode på grunn av stans
            saksbehandler.endreOpplysning(harLøpendeRett, true, "", Gyldighetsperiode(tom = 7.juli(2018)))
            saksbehandler.endreOpplysning(harLøpendeRett, false, "", Gyldighetsperiode(8.juli(2018)))

            // Verifiser at behandlingen nå bare beregner et subset av meldeperioden
            klumpen {
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 28

                    // Første dag i ny meldeperiode
                    this.first { it.status != Arvet }.gyldigFraOgMed shouldBe 2.juli(2018)

                    // Siste dag i ny meldeperiode
                    this.last().gyldigFraOgMed shouldBe 15.juli(2018)

                    // Siste dag med forbruk (8. juli er søndag, så siste forbruksdag blir fredag 6. juli)
                    this.last { it.verdi.verdi == "true" }.gyldigFraOgMed shouldBe 6.juli(2018)
                }
            }
        }
    }
}
