package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.helpers.scenario.assertions.Opplysningsperiode
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.behandling.mai
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.beregning.Beregning
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BeregningTest {
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

            behandlingsresultatForslag {
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 14

                    // Første dag i meldekort
                    this.first().gyldigFraOgMed shouldBe 18.juni(2018)

                    // Første forbruksdag
                    this.first { it.verdi.verdi == true }.gyldigFraOgMed shouldBe 21.juni(2018)

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
                    this.last { it.verdi.verdi == true }.gyldigFraOgMed shouldBe 13.juli(2018)
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
                    this.last { it.verdi.verdi == true }.gyldigFraOgMed shouldBe 6.juli(2018)
                }
            }
        }
    }

    @Test
    fun `meldekort blir satt på vent når det ikke er overlappende rettighetsperioder`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            // Send inn meldekort før vedtak
            person.sendInnMeldekort(Periode(28.mai(2018), 10.juni(2018)))
            person.sendInnMeldekort(Periode(11.juni(2018), 24.juni(2018)))

            // Systemet kjører beregningsbatchen
            meldekortBatch()

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            vedtak { utfall shouldBe true }

            // Send inn meldekort
            person.sendInnMeldekort(Periode(25.juni(2018), 8.juli(2018)))

            // Systemet kjører beregningsbatchen
            meldekortBatch(true)
            meldekortBatch(true)
        }
    }

    @Test
    fun `vi kan reberegne meldekort når de korrigeres (forrige periode)`() {
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
            val meldekortId = person.sendInnMeldekort(1)

            // Systemet kjører beregningsbatchen
            meldekortBatch(true)

            behandlingsresultat {
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 14

                    // Første dag i meldekort
                    this.first().gyldigFraOgMed shouldBe 18.juni(2018)

                    // Første forbruksdag
                    this.first { it.verdi.verdi == true }.gyldigFraOgMed shouldBe 21.juni(2018)

                    // Siste dag i meldekort
                    this.last().gyldigFraOgMed shouldBe 1.juli(2018)
                }

                with(opplysninger(Beregning.utbetalingForPeriode)) {
                    first().verdi.verdi shouldBe 5036
                }
            }

            // Send inn korrigering av forrige meldekort
            person.sendInnMeldekort(1, korrigeringAv = meldekortId, timer = List(14) { 7 })

            // Systemet kjører beregningsbatchen
            meldekortBatch()

            // Vi lager et forslag om reberegning av forrige periode
            behandlingsresultatForslag {
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 14

                    // Ingen opplysninger om forbruk skal være arvet
                    this.none { it.status == Opplysningsperiode.Periodestatus.Arvet } shouldBe true

                    // Første dag i ny meldeperiode
                    this.first().gyldigFraOgMed shouldBe 18.juni(2018)

                    // Nå er det jobbet over terskel og det skal ikke være noen forbruksdager
                    this.none { it.verdi.verdi == true } shouldBe true

                    // Siste dag i meldekort
                    this.last().gyldigFraOgMed shouldBe 1.juli(2018)
                }

                with(opplysninger(Beregning.utbetalingForPeriode)) {
                    first().verdi.verdi shouldBe 0
                }
            }
        }
    }

    @Test
    @Disabled("Dette eksploderer fullstendig på grunn av utenErstattet() i Opplysninger")
    fun `vi kan reberegne meldekort når de korrigeres (tidligere periode)`() {
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
            val meldekortId = person.sendInnMeldekort(2)
            person.sendInnMeldekort(3)

            // Systemet kjører beregningsbatchen
            meldekortBatch(true)
            meldekortBatch(true)
            meldekortBatch(true)

            behandlingsresultat {
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 42

                    // Første dag i meldekort
                    this.first().gyldigFraOgMed shouldBe 18.juni(2018)

                    // Første forbruksdag
                    this.first { it.verdi.verdi == true }.gyldigFraOgMed shouldBe 21.juni(2018)

                    // Siste dag i meldekort
                    this.last().gyldigFraOgMed shouldBe 29.juli(2018)
                }

                with(opplysninger(Beregning.utbetalingForPeriode)) {
                    this shouldHaveSize 3
                    this[0].verdi.verdi shouldBe 5036
                    this[1].verdi.verdi shouldBe 12590
                    this[2].verdi.verdi shouldBe 12590
                }
            }

            val sisteBehandlingId = person.behandlingId

            // Send inn korrigering av forrige meldekort
            person.sendInnMeldekort(2, korrigeringAv = meldekortId, timer = List(14) { 7 })

            // Systemet kjører beregningsbatchen
            meldekortBatch()

            // Vi lager et forslag om reberegning av forrige periode
            behandlingsresultatForslag {
                basertPå shouldBe sisteBehandlingId

                with(opplysninger(Beregning.utbetalingForPeriode)) {
                    this shouldHaveSize 3
                    this[0].verdi.verdi shouldBe 5036
                    this[1].verdi.verdi shouldBe 0
                    this[2].verdi.verdi shouldBe 12590
                }
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 42

                    // Ingen opplysninger om forbruk skal være arvet
                    this.none { it.status == Opplysningsperiode.Periodestatus.Arvet } shouldBe true

                    // Første dag i ny meldeperiode
                    this.first().gyldigFraOgMed shouldBe 18.juni(2018)

                    // Nå er det jobbet over terskel og det skal ikke være noen forbruksdager
                    this.none { it.verdi.verdi == true } shouldBe true

                    // Siste dag i meldekort
                    this.last().gyldigFraOgMed shouldBe 1.juli(2018)
                }
            }
        }
    }
}
