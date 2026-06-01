package no.nav.dagpenger.scenario

import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Test

class TidsbegrensetBortfallTest {
    @Test
    fun `tidsbegrenset bortfall markerer dager og reduserer utbetaling`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            // Saksbehandler ilegger tidsbegrenset bortfall på 5 dager
            saksbehandler.endreOpplysning(TidsbegrensetBortfall.harTidsbegrensetBortfall, true)
            saksbehandler.endreOpplysning(TidsbegrensetBortfall.antallBortfallsdager, 5)

            saksbehandler.endreOpplysning(Sanksjonsperiode.harSanksjon, true)
            saksbehandler.endreOpplysning(Sanksjonsperiode.antallSanksjonsdager, 2)

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder.single().harRett shouldBe true
            }

            // Meldekort 1 (18.juni - 1.juli): 7 arbeidsdager med rett
            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(2) {
                // Stønadsdager forbrukes normalt (alle 7 arbeidsdager)
                with(opplysninger(Beregning.forbruk)) {
                    count { it.verdi.verdi == true } shouldBe 7
                }

                with(opplysninger(Beregning.gjenståendeBortfallsdager)) {
                    last().verdi.verdi shouldBe 0
                }
                with(opplysninger(Beregning.gjenståendeSanksjonsdager)) {
                    last().verdi.verdi shouldBe 0
                }

                // 7 dager markert som bortfall (5 + 2 fra to samtidige sanksjoner)
                with(opplysninger(Beregning.erSanksjonsdag)) {
                    count { it.verdi.verdi == true } shouldBe 7
                }

                // Utbetaling er vesentlig lavere enn uten bortfall (5036)
                // Med 5 bortfallsdager + egenandel som tar resten → 0 kr
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

            // Saksbehandler ilegger tidsbegrenset bortfall på 3 dager
            saksbehandler.endreOpplysning(TidsbegrensetBortfall.harTidsbegrensetBortfall, true)
            saksbehandler.endreOpplysning(TidsbegrensetBortfall.antallBortfallsdager, 3)

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
                    // Bortfall-dager telles ikke mot rettighets-kvoten — kun 7 rettighetsdager telles
                    this.last().verdi.verdi shouldBe 7
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
}
