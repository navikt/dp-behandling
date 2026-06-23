package no.nav.dagpenger.scenario

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.dato.februar
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.mediator.api.models.OpprinnelseDTO
import no.nav.dagpenger.mediator.api.models.RettighetsperiodeDTO
import no.nav.dagpenger.mediator.august
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.mediator.mai
import no.nav.dagpenger.mediator.november
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage
import no.nav.dagpenger.regel.regelsett.vilkår.Gjenopptak
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.Opptjeningstid
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalGjenopptakVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning.kravTilUtdanning
import no.nav.dagpenger.scenario.MeldekortAktivitet.Arbeid
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class OmgjøringScenarioTester {
    @Test
    @Disabled("Fungerer ikke i main (enda)")
    fun `omgjøring av førstegangs innvilgelse tilbake i tid`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            // Søk og innvilg dagpenger
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe true

                opplysninger(Opptjeningstid.sisteAvsluttendendeKalenderMåned).single().verdi.verdi shouldBe 31.mai(2018).toString()
            }
            val opprinneligSøknadId = person.sisteSøknadId!!

            // Utfør omgjøring
            saksbehandler.omgjørBehandling(30.juni(2018))
            saksbehandler.endreOpplysning(
                søknadIdOpplysningstype,
                opprinneligSøknadId.toString(),
                "Bak i tid",
                Gyldighetsperiode(1.mai(2018)),
            )
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(
                Søknadstidspunkt.prøvingsdato,
                1.mai(2018),
                "Bak i tid",
                Gyldighetsperiode(1.mai(2018)),
            )
            behovsløsere.løsTilForslag()

            // saksbehandler.endreOpplysning(OmgjøringUtenKlage.ansesUgyldigVedtak, true)

            // Verifiser at behandlingen har beregnet utbetaling for perioden
            behandlingsresultatForslag(5) {
                opplysninger(Opptjeningstid.sisteAvsluttendendeKalenderMåned).single().verdi.verdi shouldNotBe 31.mai(2018).toString()
            }
        }
    }

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
            meldekortBatch(markerFerdig = true)

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
            meldekortBatch(markerFerdig = true)
            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

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

            behandlingsresultat {
                with(opplysninger(Gjenopptak.skalGjenopptas)) {
                    this shouldHaveSize 1
                    this.single().gyldigFraOgMed shouldBe 8.august(2018)
                }
            }

            person.sendInnMeldekort(3)
            meldekortBatch(markerFerdig = true)
            person.sendInnMeldekort(4)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 21403
            }

            // Omgjøring
            saksbehandler.omgjørBehandling(1.august(2018))
            saksbehandler.endreOpplysning(
                skalGjenopptakVurderes,
                true,
                "Endrer kravet om gjenopptak til 1. august, som er første mulige dato for gjenopptak",
                Gyldighetsperiode(1.august(2018)),
            )
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
                with(opplysninger(Gjenopptak.skalGjenopptas)) {
                    this shouldHaveSize 1
                    this.single().gyldigFraOgMed shouldBe 1.august(2018)
                }
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 27698
                rettighetsperioder shouldHaveSize 3
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[2].harRett shouldBe true
            }

            person.sendInnMeldekort(5)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBeGreaterThan 27698
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 40288
                behandletHendelse["type"].asString() shouldBe "Meldekort"
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
            meldekortBatch(markerFerdig = true)
            person.sendInnMeldekort(2)

            // Stans
            saksbehandler.lagBehandling(25.januar(2018))
            saksbehandler.endreOpplysning(Opphold.oppholdINorge, false, "Oppholder seg ikke i Norge", Gyldighetsperiode(25.januar(2018)))
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Beregn meldekort etter stans
            meldekortBatch(markerFerdig = true)

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
            meldekortBatch(markerFerdig = true)
            person.sendInnMeldekort(4)
            meldekortBatch(markerFerdig = true)

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
                rettighetsperioder shouldHaveSize 4
                rettighetsperioder shouldContainExactly
                    listOf(
                        RettighetsperiodeDTO(1.januar(2018), 24.januar(2018), true, opprinnelse = OpprinnelseDTO.ARVET),
                        RettighetsperiodeDTO(25.januar(2018), 13.februar(2018), false, opprinnelse = OpprinnelseDTO.ARVET),
                        RettighetsperiodeDTO(14.februar(2018), 18.februar(2018), false, opprinnelse = OpprinnelseDTO.NY),
                        RettighetsperiodeDTO(19.februar(2018), null, true, opprinnelse = OpprinnelseDTO.NY),
                    )

                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 42
                    // Onsdag, torsdag og fredag i uke 7 skal falle ut og ikke være forbruksdager
                    this.count { it.verdi.verdi == true } shouldBe 23
                }

                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBeLessThan 27991
            }
        }
    }

    @Test
    fun `behandling av meldekort når de korrigerer en periode for langt bak i tid`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat { rettighetsperioder.last().harRett shouldBe true }

            // Send inn meldekort
            person.sendInnMeldekort(1)
            val meldekortId = person.sendInnMeldekort(2)
            person.sendInnMeldekort(3)

            // Systemet kjører beregningsbatchen
            meldekortBatch(markerFerdig = true)
            meldekortBatch(markerFerdig = true)
            meldekortBatch(markerFerdig = true)

            // Send inn korrigering av forrige meldekort
            person.sendInnMeldekort(2, korrigeringAv = meldekortId, timer = List(14) { 7 })

            // Systemet kjører beregningsbatchen
            meldekortBatch()

            person.avklaringer.first().kode shouldBe "KorrigeringUtbetaltPeriode"

            shouldNotThrow<IllegalArgumentException> {
                saksbehandler.lukkAlleAvklaringer()
            }
            behandlingsresultat { rettighetsperioder.last().harRett shouldBe true }
        }
    }

    @Test
    fun `Omgjør en behandling før vi har rett på den første`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(13.mai(2026))
            behovsløsere.løsTilForslag()
            saksbehandler.avbryt()

            person.søkDagpenger(15.mai(2026), 28.mai(2026))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder.last().harRett shouldBe true
                rettighetsperioder.last().fraOgMed shouldBe 28.mai(2026)
            }

            // Omgjøring
            saksbehandler.omgjørBehandling(27.mai(2018))
        }
    }

    @Test
    fun `omgjøring av stans etter utdanning på meldekort`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.november(2025))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder.last().harRett shouldBe true
                rettighetsperioder.last().fraOgMed shouldBe 21.november(2025)
            }

            repeat(14) {
                person.sendInnMeldekort(1)
                meldekortBatch(markerFerdig = true)
            }

            person.sendInnMeldekort(
                15,
                aktiviteter =
                    listOf(
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        Arbeid(0),
                        MeldekortAktivitet.Utdanning(0),
                    ),
            )
            meldekortBatch(markerFerdig = true)

            behandlingsresultatForslag(3) {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe false
                rettighetsperioder.last().fraOgMed shouldBe 8.juni(2026)
            }
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            behandlingsresultat(16) {
                opplysninger(Utdanning.tarUtdanning) {
                    this[0].verdi.verdi shouldBe false
                    this[1].verdi.verdi shouldBe true
                    this[1].gyldigFraOgMed shouldBe 8.juni(2026)
                }
            }

            // Omgjøring
            saksbehandler.omgjørBehandling(22.juni(2026))
            saksbehandler.endreOpplysning(
                Utdanning.deltakelseIArbeidsmarkedstiltak,
                true,
                "Har litt tiltak",
                Gyldighetsperiode(8.juni(2026), 3.juli(2026)),
            )
            saksbehandler.endreOpplysning(
                Utdanning.tarUtdanning,
                false,
                "Tiltak er ferdig",
                Gyldighetsperiode(4.juli(2026)),
            )

            behandlingsresultatForslag(7) {
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder[0].harRett shouldBe true

                opplysninger(kravTilUtdanning) {
                    this shouldHaveSize 3
                    this[0].verdi.verdi shouldBe true
                    this[0].gyldigFraOgMed shouldBe 21.november(2025)

                    this[1].verdi.verdi shouldBe true
                    this[1].gyldigFraOgMed shouldBe 8.juni(2026)

                    this[2].verdi.verdi shouldBe true
                    this[2].gyldigFraOgMed shouldBe 4.juli(2026)
                }
            }
        }
    }
}
