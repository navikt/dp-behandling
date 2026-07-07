package no.nav.dagpenger.scenario

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.forbruktSanksjonsdager
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.gjenståendeSanksjonsdager
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.sisteGjenståendeDager
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.sisteGjenståendeSanksjonsdager
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Test

class SanksjonTest {
    @Test
    fun `ilegges sanksjonsperiode ved selvforskyldt arbeidsløshet`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(18.juni(2018))
            behovsløsere.løsTilForslag()

            // Saksbehandler ilegger sanksjonsperiode
            saksbehandler.endreOpplysning(Sanksjonsperiode.harSanksjon, true)

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder.single().harRett shouldBe true
            }

            // Meldekort 1 (18.juni - 1.juli): 01 arbeidsdager med rett
            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat {
                // Forbruker ikke egenandel før sanksjon er avviklet
                opplysninger(Beregning.forbruktEgenandel) {
                    this.sumOf { it.verdi.verdi as Int } shouldBe 0
                }

                // Stønadsdager forbrukes normalt (alle 10 arbeidsdager)
                with(opplysninger(Beregning.forbruk)) {
                    count { it.verdi.verdi == true } shouldBe 10
                }

                // Teller forbruk
                with(opplysninger(Beregning.forbrukt)) {
                    this.last().verdi.verdi shouldBe 10
                }

                // Teller gjenstående
                with(opplysninger(Beregning.sisteGjenståendeDager)) {
                    this.last().verdi.verdi shouldBe 510
                }

                // 10 dager markert som sanksjon
                with(opplysninger(Beregning.erSanksjonsdag)) {
                    count { it.verdi.verdi == true } shouldBe 10
                }

                // All utbetaling faller bort på grunn av bortfall
                val totalUtbetaling = utbetalinger.sumOf { it["utbetaling"].asInt() }
                totalUtbetaling shouldBe 0
            }
        }
    }

    @Test
    fun `bortfall utløper og neste meldekort har normal utbetaling`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(18.juni(2018))
            behovsløsere.løsTilForslag()

            // Bruker har 3 dager med sanksjon igjen
            saksbehandler.endreOpplysning(Sanksjonsperiode.harSanksjon, true)
            saksbehandler.endreOpplysning(Sanksjonsperiode.antallSanksjonsdager, 3)

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder.single().harRett shouldBe true
            }

            // Meldekort 1: 3 bortfallsdager, 4 normale dager
            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(2) {
                with(opplysninger(Beregning.erSanksjonsdag)) {
                    count { it.verdi.verdi == true } shouldBe 3
                }
                with(opplysninger(Beregning.forbrukt)) {
                    // Sanksjons-dager telles mot rettighets-kvoten
                    this.last().verdi.verdi shouldBe 10
                }
                // 4 dager med utbetaling (minus egenandel)
                utbetalinger.count { it["utbetaling"].asInt() > 0 } shouldBeGreaterThan 0
            }

            // Meldekort 2: bortfall er brukt opp, alle dager normalt
            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(3) {
                // Ingen bortfallsdager i meldekort 2
                with(opplysninger(Beregning.erSanksjonsdag)) {
                    filter { it.gyldigFraOgMed != null && it.gyldigFraOgMed >= 2.juli(2018) }
                        .all { it.verdi.verdi == false } shouldBe true
                }

                // Meldekort 2 har utbetaling > 0 (egenandel allerede brukt opp)
                utbetalinger
                    .filter { it["dato"].asString() >= "2018-07-02" }
                    .sumOf { it["utbetaling"].asInt() } shouldBeGreaterThan 0
            }
        }
    }

    @Test
    fun `uten bortfall gir normal utbetaling`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(2) {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 5036
            }
        }
    }

    @Test
    fun `ilegges sanksjonsperiode ved selvforskyldt arbeidsløshet med revurderes og lages et omgjøringsvedtak`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(18.juni(2018))
            behovsløsere.løsTilForslag()

            // Saksbehandler ilegger sanksjonsperiode
            saksbehandler.endreOpplysning(Sanksjonsperiode.harSanksjon, true)

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder.single().harRett shouldBe true
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            person.sendInnMeldekort(3)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat {
                // Forbruker ikke egenandel før sanksjon er avviklet
                opplysninger(Beregning.forbruktEgenandel) {
                    this.sumOf { it.verdi.verdi as Int } shouldBe 0
                }

                // Stønadsdager forbrukes normalt (alle 10 arbeidsdager)
                with(opplysninger(Beregning.forbruk)) {
                    count { it.verdi.verdi == true } shouldBe 30
                }

                // Teller forbruk
                with(opplysninger(Beregning.forbrukt)) {
                    this.last().verdi.verdi shouldBe 30
                }

                // Teller gjenstående
                with(opplysninger(Beregning.sisteGjenståendeDager)) {
                    //      this.last().verdi.verdi shouldBe 490
                }

                // 10 dager markert som sanksjon
                with(opplysninger(Beregning.erSanksjonsdag)) {
                    count { it.verdi.verdi == true } shouldBe 30
                }

                // All utbetaling faller bort på grunn av bortfall
                val totalUtbetaling = utbetalinger.sumOf { it["utbetaling"].asInt() }
                totalUtbetaling shouldBe 0
            }

            saksbehandler.omgjørBehandling(30.juni(2018))
            // Saksbehandler ilegger sanksjonsperiode
            saksbehandler.endreOpplysning(Sanksjonsperiode.harSanksjon, false)
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(5) {
                rettighetsperioder.single().harRett shouldBe true
                // Forbruker ikke egenandel før sanksjon er avviklet
                opplysninger(Beregning.forbruktEgenandel) {
                    this.sumOf { it.verdi.verdi as Int } shouldBeGreaterThan 0
                }

                // Stønadsdager forbrukes normalt (alle 10 arbeidsdager)
                with(opplysninger(Beregning.forbruk)) {
                    count { it.verdi.verdi == true } shouldBe 30
                }

                // Teller forbruk
                with(opplysninger(Beregning.forbrukt)) {
                    this.last().verdi.verdi shouldBe 30
                }

                // Teller gjenstående
                with(opplysninger(Beregning.sisteGjenståendeDager)) {
//                    this.last().verdi.verdi shouldBe 490
                }

                // 0 dager markert som sanksjon
                with(opplysninger(Beregning.erSanksjonsdag)) {
                    count { it.verdi.verdi == true } shouldBe 0
                }

                // Sanksjonsdager skal være nullstilt etter omgjøring
                with(opplysninger(forbruktSanksjonsdager)) {
                    size shouldBe 42
                    all { it.verdi.verdi == 0 } shouldBe true
                }

                // Gjenstående sanksjonsdager skal være nullstilt etter omgjøring
                with(opplysninger(gjenståendeSanksjonsdager)) {
                    size shouldBe 42
                    all { it.verdi.verdi == 0 } shouldBe true
                }

                // Siste Gjenstående sanksjonsdag skal være nullstilt etter omgjøring
                opplysninger(sisteGjenståendeSanksjonsdager) {
                    size shouldBe 0
                    // all { it.verdi.verdi == 0 } shouldBe true
                }

                // Det skal bli utbetalt
                val totalUtbetaling = utbetalinger.sumOf { it["utbetaling"].asInt() }
                totalUtbetaling shouldBeGreaterThan 0
            }
        }
    }
}
