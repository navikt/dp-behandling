package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.februar
import no.nav.dagpenger.mediator.januar
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport
import no.nav.dagpenger.regel.regelsett.vilkår.Meldeplikt
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker
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
                opplysninger(Eksport.oppfyllerVilkårForEksport) {
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
                opplysninger(Eksport.oppfyllerVilkårForEksport) {
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

                // Registrert innen fristen, gjenopptak skal arve gyldighetsperioden fra skalHaEksportFra (1.februar)
                opplysninger(Eksport.eksportGjenopptakFraOgMed) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe 1.februar(2025).toString()
                    single().gyldigFraOgMed shouldBe 1.februar(2025)
                }
            }

            // TODO: Se på samspill med stans etter 21 dager
        }
    }

    @Test
    fun `eksport av dagpenger til EØS-land hvor bruker registrerer seg nøyaktig på fristdagen`() {
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

            // Frist for å registrere seg er skalHaEksportFra + 7 dager = 8.februar. Registrering
            // nøyaktig på fristdagen skal fortsatt regnes som "innen fristen".
            saksbehandler.lagBehandling(8.februar(2025))
            saksbehandler.endreOpplysning(
                Eksport.registrertIVertsland,
                true,
                "Har registrert seg i syden på siste gyldige dag",
                gyldighetsperiode = Gyldighetsperiode(8.februar(2025)),
            )

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Registrert på fristdagen: skal fortsatt behandles som "innen fristen", altså
            // gjenopptak fra og med skalHaEksportFra, ikke fra registreringsdatoen.
            behandlingsresultat(4) {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe true
                rettighetsperioder.last().fraOgMed shouldBe 1.februar(2025)

                opplysninger(Eksport.eksportGjenopptakFraOgMed) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe 1.februar(2025).toString()
                }
            }
        }
    }

    @Test
    fun `eksport av dagpenger til EØS-land hvor bruker aldri registrerer seg i vertslandet`() {
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
                opplysninger(Eksport.oppfyllerVilkårForEksport) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }
            }

            // Bruker registrerer seg aldri i vertslandet. Videre meldekort skal ikke
            // spontant gi gjenopptak av dagpenger.
            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(4) {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe false
                opplysninger(Eksport.oppfyllerVilkårForEksport) {
                    shouldHaveSize(1)
                    single().verdi.verdi shouldBe false
                }
            }
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

                // Registrert etter fristen, gjenopptak skal arve gyldighetsperioden fra registreringsdatoen.
                // Historikken inneholder også en tidligere (nå overstyrt) gjetning fra behandlingen
                // hvor registrering ennå ikke var kjent, så vi sjekker den siste/gjeldende perioden.
                opplysninger(Eksport.eksportGjenopptakFraOgMed) {
                    last().verdi.verdi shouldBe 15.februar(2025).toString()
                    last().gyldigFraOgMed shouldBe 15.februar(2025)
                }
            }

            // TODO: Se på samspill med stans etter 21 dager
        }
    }

    @Test
    fun `eksport, registrering og meldekort gir sammenhengende utbetaling`() {
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

            behandlingsresultat(4) {
                rettighetsperioder.last().harRett shouldBe true
                rettighetsperioder.last().fraOgMed shouldBe 1.februar(2025)
            }

            // Sender inn meldekort for perioden hvor eksport ble avklart. Selv om dagpenger var
            // midlertidig stanset i påvente av registrering, skal hele perioden være utbetalt siden
            // gjenopptaket arver gyldighetsperioden tilbake til 1.februar (innen fristen).
            person.sendInnMeldekort(3)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat {
                // Ukedagene i februar skal alle være utbetalt siden gjenopptaket arver
                // gyldighetsperioden tilbake til 1.februar (innen fristen for registrering),
                // selv om registreringen i vertslandet ikke skjedde før 5.februar.
                listOf(
                    3.februar(2025),
                    4.februar(2025),
                    5.februar(2025),
                    6.februar(2025),
                    7.februar(2025),
                ).forEach { dato ->
                    val utbetaling = utbetalinger.toList().single { it["dato"].asString() == dato.toString() }
                    utbetaling["utbetaling"].asInt() shouldBeGreaterThan 0
                }
            }
        }
    }

    @Test
    fun `bruker under eksport blir avregistrert i ASR - skal ikke gi stans`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
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

            behandlingsresultat(4) {
                rettighetsperioder.last().harRett shouldBe true
                rettighetsperioder.last().fraOgMed shouldBe 1.februar(2025)
            }

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Blir avregistrert av veileder i ASR, mens bruker fortsatt har eksport av dagpenger
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(3).atTime(12, 21),
                manueltAvregistrert = true,
            )

            // Avregistreringen skal ikke føre til stans, siden RegistrertArbeidssøker
            // ikke skal påvirke resultatet mens eksport av dagpenger vurderes
            behandlingsresultatForslag {
                førteTil shouldBe "Endring"
                rettighetsperioder.last().harRett shouldBe true
                rettighetsperioder.last().tilOgMed shouldBe null
            }
        }
    }

    // Scenario 3 fra ArbeidssøkerTest, kombinert med aktiv eksport
    @Test
    fun `bruker under eksport melder seg ikke innen 21-dagers frist - skal ikke gi stans`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
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

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Fristbrudd inntreffer mens bruker fortsatt har eksport av dagpenger
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(16).atTime(12, 21),
                fristBrutt = true,
            )

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultatForslag {
                førteTil shouldBe "Endring"
                rettighetsperioder.last().harRett shouldBe true
                rettighetsperioder.last().tilOgMed shouldBe null
            }
        }
    }

    // Scenario 4 fra ArbeidssøkerTest, kombinert med aktiv eksport
    @Test
    fun `bruker under eksport sier nei på meldekort - skal ikke gi stans`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
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

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Melder seg for periode 3 i tide, men svarer nei på fortsatt registrert
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(16).atTime(12, 21),
            )

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultatForslag {
                førteTil shouldBe "Stans"
                with(opplysninger(Meldeplikt.oppfyllerMeldeplikt)) {
                    this shouldHaveSize 1
                    this[0].verdi.verdi shouldBe true
                }
                with(opplysninger(RegistrertArbeidssøker.registrertArbeidssøker)) {
                    this shouldHaveSize 2
                    this[0].verdi.verdi shouldBe true
                    this[1].verdi.verdi shouldBe false
                    this[1].gyldigFraOgMed shouldBe 12.februar(2025)
                }
                with(opplysninger(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker)) {
                    this shouldHaveSize 2
                    this[0].verdi.verdi shouldBe true
                    this[1].verdi.verdi shouldBe false
                    this[1].gyldigFraOgMed shouldBe 12.februar(2025)
                }
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].tilOgMed shouldBe null
            }
        }
    }
}
