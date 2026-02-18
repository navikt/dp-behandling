package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.behandling.august
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.dato.februar
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.prosessvilkår.OmgjøringUtenKlage
import org.junit.jupiter.api.Test

class OmgjøringScenarioTester {
    @Test
    fun `omgjøring av behandling beregner alle meldeperioder på nytt med nytt grunnlag`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            // Søk og innvilg dagpenger
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat { rettighetsperioder.single().harRett shouldBe true }

            // Send inn og behandle meldekort
            person.sendInnMeldekort(1)
            meldekortBatch(true)

            // Verifiser opprinnelig utbetaling
            behandlingsresultat {
                with(opplysninger(Beregning.utbetalingForPeriode)) {
                    first().verdi.verdi shouldBe 5036
                }
                with(opplysninger(dagsatsEtterSamordningMedBarnetillegg)) {
                    this[0].verdi.verdi shouldBe 1259
                }
            }

            // Utfør omgjøring
            saksbehandler.omgjørBehandling(21.juni(2018))
            saksbehandler.endreOpplysning(
                DagpengenesStørrelse.dagsatsUtenBarnetillegg,
                Beløp(1000000000.0),
                "Mere Penger!",
                Gyldighetsperiode(21.juni(2018)),
            )

            // Verifiser at omgjøringsbehandlingen har avklaring
            person.avklaringer.first().kode shouldBe "HarSvartPåOmgjøringUtenKlage"

            saksbehandler.endreOpplysning(
                OmgjøringUtenKlage.ansesUgyldigVedtak,
                true,
            )

            // Verifiser at behandlingen har beregnet utbetaling for perioden
            behandlingsresultatForslag {
                with(opplysninger(dagsatsEtterSamordningMedBarnetillegg)) {
                    this shouldHaveSize 1
                    this[0].verdi.verdi shouldBe 1791
                }
                with(opplysninger(Beregning.utbetalingForPeriode)) {
                    // Utbetalingen skal fortsatt være beregnet (med samme verdi siden grunnlaget er likt)
                    single().verdi.verdi shouldNotBe 5036
                    single().verdi.verdi shouldBe 8760
                }
            }
        }
    }

    @Test
    fun `omgjøring med endring av rettighetsperide, men fortsatt dekket av meldekort`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            // Søk og innvilg dagpenger
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Send inn og behandle meldekort for flere perioder
            person.sendInnMeldekort(1)
            meldekortBatch(true)
            person.sendInnMeldekort(2)
            meldekortBatch(true)

            // Stans
            saksbehandler.lagBehandling(11.juli(2018))
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, false, "Oppholder seg ikke i Norge", Gyldighetsperiode(15.juli(2018)))
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[1].harRett shouldBe false
            }

            // Gjenopptak
            person.søkGjenopptak(8.august(2018))
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, true, "Oppholder seg i Norge", Gyldighetsperiode(8.august(2018)))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(3)
            meldekortBatch(true)
            person.sendInnMeldekort(4)
            meldekortBatch(true)

            behandlingsresultat {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 21403
            }

            // Omgjøring
            saksbehandler.omgjørBehandling(1.august(2018))
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, true, "Oppholder seg i Norge", Gyldighetsperiode(1.august(2018)))
            saksbehandler.endreOpplysning(
                OmgjøringUtenKlage.ansesUgyldigVedtak,
                true,
            )
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 27698
                rettighetsperioder shouldHaveSize 3
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[2].harRett shouldBe true
            }
        }
    }

    @Test
    fun `omgjøring med endring av rettighetsperide, og dager uten rett forsvinner som forbruksdager`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            // Søk og innvilg dagpenger
            person.søkDagpenger(1.januar(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Send inn og behandle meldekort for flere perioder
            person.sendInnMeldekort(1)
            meldekortBatch(true)
            person.sendInnMeldekort(2)

            // Stans
            saksbehandler.lagBehandling(25.januar(2018))
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, false, "Oppholder seg ikke i Norge", Gyldighetsperiode(25.januar(2018)))
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Beregn meldekort etter stans
            meldekortBatch(true)

            behandlingsresultat {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].tilOgMed shouldBe 24.januar(2018)
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 25.januar(2018)

                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 28
                    this.count { it.verdi.verdi == true } shouldBe 18
                }
            }

            // Gjenopptak
            person.søkGjenopptak(14.februar(2018))
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, true, "Oppholder seg i Norge", Gyldighetsperiode(14.februar(2018)))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(3)
            meldekortBatch(true)
            person.sendInnMeldekort(4)
            meldekortBatch(true)

            behandlingsresultat {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 27991

                rettighetsperioder shouldHaveSize 3
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].tilOgMed shouldBe 24.januar(2018)
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 25.januar(2018)
                rettighetsperioder[2].harRett shouldBe true
                rettighetsperioder[2].fraOgMed shouldBe 14.februar(2018)

                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 42 // Meldekort 1, 2, og 4 blir beregnet.
                    this.count { it.verdi.verdi == true } shouldBe 26
                }
            }

            // Omgjøring
            saksbehandler.omgjørBehandling(19.februar(2018))
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, true, "Oppholder seg i Norge", Gyldighetsperiode(19.februar(2018)))
            saksbehandler.endreOpplysning(
                Opphold.oppholdINorge,
                false,
                "Oppholder seg ikke i Norge",
                Gyldighetsperiode(14.februar(2018), 18.februar(2018)),
            )
            saksbehandler.endreOpplysning(
                OmgjøringUtenKlage.ansesUgyldigVedtak,
                true,
            )
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder shouldHaveSize 3
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 25.januar(2018)
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].tilOgMed shouldBe 18.februar(2018)
                rettighetsperioder[2].harRett shouldBe true
                rettighetsperioder[2].fraOgMed shouldBe 19.februar(2018)

                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 42
                    // Onsdag, torsdag og fredag i uke 7 skal falle ut og ikke være forbruksdager
                    this.count { it.verdi.verdi == true } shouldBe 23
                }

                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBeLessThan 27991
            }
        }
    }
}
