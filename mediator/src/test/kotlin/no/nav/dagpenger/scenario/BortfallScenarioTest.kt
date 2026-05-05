package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.regel.TidsbegrensetBortfall
import no.nav.dagpenger.regel.beregning.Beregning
import org.junit.jupiter.api.Test

class BortfallScenarioTest {
    @Test
    fun `tidsbegrenset bortfall markerer dager og reduserer utbetaling`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            // Saksbehandler ilegger tidsbegrenset bortfall på 5 dager
            saksbehandler.endreOpplysning(TidsbegrensetBortfall.harBortfall, true)
            saksbehandler.endreOpplysning(TidsbegrensetBortfall.antallBortfallsdager, 5)

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe true
            }

            // Meldekort 1 (18.juni - 1.juli): 7 arbeidsdager med rett
            person.sendInnMeldekort(1)
            meldekortBatch(true)

            behandlingsresultat {
                // Stønadsdager forbrukes normalt (alle 7 arbeidsdager)
                with(opplysninger(Beregning.forbruk)) {
                    count { it.verdi.verdi == true } shouldBe 7
                }

                // 5 dager markert som bortfall (de 5 tidligste arbeidsdagene)
                with(opplysninger(Beregning.erBortfallsdag)) {
                    count { it.verdi.verdi == true } shouldBe 5
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
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            // Saksbehandler ilegger tidsbegrenset bortfall på 3 dager
            saksbehandler.endreOpplysning(TidsbegrensetBortfall.harBortfall, true)
            saksbehandler.endreOpplysning(TidsbegrensetBortfall.antallBortfallsdager, 3)

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe true
            }

            // Meldekort 1: 3 bortfallsdager, 4 normale dager
            person.sendInnMeldekort(1)
            meldekortBatch(true)

            behandlingsresultatForslag {
                with(opplysninger(Beregning.erBortfallsdag)) {
                    count { it.verdi.verdi == true } shouldBe 3
                }
                // 4 dager med utbetaling (minus egenandel)
                utbetalinger.count { it["utbetaling"].asInt() > 0 } shouldBeGreaterThan 0
            }

            // Meldekort 2: bortfall er brukt opp, alle dager normalt
            person.sendInnMeldekort(2)
            meldekortBatch(true)

            behandlingsresultatForslag {
                // Ingen bortfallsdager i meldekort 2
                with(opplysninger(Beregning.erBortfallsdag)) {
                    filter {
                        it.gyldigFraOgMed != null &&
                            it.gyldigFraOgMed!! >= 2.juli(2018)
                    }.all { it.verdi.verdi == false } shouldBe true
                }

                // Meldekort 2 har utbetaling > 0 (egenandel allerede brukt opp)
                utbetalinger
                    .filter { it["dato"].asText() >= "2018-07-02" }
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
            meldekortBatch(true)

            behandlingsresultatForslag {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 5036
            }
        }
    }
}
