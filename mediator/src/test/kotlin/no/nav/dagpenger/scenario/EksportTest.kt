package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldContain
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
                rettighetsperioder {
                    fra(1.januar(2025)) harRett true
                }
                opplysning(Rettighetstype.skalEksportVurderes) {
                    fra(1.januar(2025)) verdi false
                }
                opplysning(Eksport.oppfyllerVilkårForEksport) {
                    erIkkeVurdert()
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(2) {
                førteTil shouldBe "Endring"
            }

            saksbehandler.lagBehandling(1.februar(2025))
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEksportVurderes,
                true,
                "Drar til syden",
                gyldighetsperiode = Gyldighetsperiode(1.februar(2025)),
            )

            behandlingsresultatForslag {
                opplysning(Rettighetstype.skalEksportVurderes) {
                    fra(1.januar(2025)) til 31.januar(2025) verdi false
                    fra(1.februar(2025)) verdi true
                }
            }

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Eksport godkjent og bruker får stans fram til registrering i vertsland
            behandlingsresultat(3) {
                opplysning(Rettighetstype.skalEksportVurderes) {
                    fra(1.januar(2025)) til 31.januar(2025) verdi false
                    fra(1.februar(2025)) verdi true
                }
                opplysning(Eksport.oppfyllerVilkårForEksport) {
                    fra(1.februar(2025)) verdi false
                }
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett false
                }
            }

            // Registrerer seg i vertslandet og skal ha gjenopptak av dagpenger
            saksbehandler.lagBehandling(5.februar(2025))
            saksbehandler.leggTilOpplysning(
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
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett true
                }

                // Registrert innen fristen, gjenopptak skal arve gyldighetsperioden fra skalHaEksportFra (1.februar)
                opplysning(Eksport.eksportGjenopptakFraOgMed) {
                    fra(1.februar(2025)) verdi 1.februar(2025)
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
            saksbehandler.leggTilOpplysning(
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
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett false
                }
            }

            // Frist for å registrere seg er skalHaEksportFra + 7 dager = 8.februar. Registrering
            // nøyaktig på fristdagen skal fortsatt regnes som "innen fristen".
            saksbehandler.lagBehandling(8.februar(2025))
            saksbehandler.leggTilOpplysning(
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
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett true
                }

                opplysning(Eksport.eksportGjenopptakFraOgMed) {
                    fra(1.februar(2025)) verdi 1.februar(2025)
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
            saksbehandler.leggTilOpplysning(
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
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett false
                }
                opplysning(Eksport.oppfyllerVilkårForEksport) {
                    fra(1.februar(2025)) verdi false
                }
            }

            // Bruker registrerer seg aldri i vertslandet. Videre meldekort skal ikke
            // spontant gi gjenopptak av dagpenger.
            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(4) {
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett false
                }
                opplysning(Eksport.oppfyllerVilkårForEksport) {
                    fra(1.februar(2025)) verdi false
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
            saksbehandler.leggTilOpplysning(
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
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett false
                }
            }

            // Registrerer seg i vertslandet og skal ha gjenopptak av dagpenger
            saksbehandler.lagBehandling(5.februar(2025))
            saksbehandler.leggTilOpplysning(
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
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) til 14.februar(2025) harRett false
                    fra(15.februar(2025)) harRett true
                }

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
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEksportVurderes,
                true,
                "Drar til syden",
                gyldighetsperiode = Gyldighetsperiode(1.februar(2025)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            saksbehandler.lagBehandling(5.februar(2025))
            saksbehandler.leggTilOpplysning(
                Eksport.registrertIVertsland,
                true,
                "Har registrert seg i syden",
                gyldighetsperiode = Gyldighetsperiode(5.februar(2025)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(4) {
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett true
                }
            }

            // Sender inn meldekort for perioden hvor eksport ble avklart. Selv om dagpenger var
            // midlertidig stanset i påvente av registrering, skal hele perioden være utbetalt siden
            // gjenopptaket arver gyldighetsperioden tilbake til 1.februar (innen fristen).
            person.sendInnMeldekort(3)
            meldekortBatch(markerFerdig = false)
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

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
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEksportVurderes,
                true,
                "Drar til syden",
                gyldighetsperiode = Gyldighetsperiode(1.februar(2025)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            saksbehandler.lagBehandling(5.februar(2025))
            saksbehandler.leggTilOpplysning(
                Eksport.registrertIVertsland,
                true,
                "Har registrert seg i syden",
                gyldighetsperiode = Gyldighetsperiode(5.februar(2025)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(4) {
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett true
                }
            }

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = false)
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

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
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett true
                }
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
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEksportVurderes,
                true,
                "Drar til syden",
                gyldighetsperiode = Gyldighetsperiode(1.februar(2025)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            saksbehandler.lagBehandling(5.februar(2025))
            saksbehandler.leggTilOpplysning(
                Eksport.registrertIVertsland,
                true,
                "Har registrert seg i syden",
                gyldighetsperiode = Gyldighetsperiode(5.februar(2025)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = false)
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Fristbrudd inntreffer mens bruker fortsatt har eksport av dagpenger
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(16).atTime(12, 21),
                fristBrutt = true,
            )

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultatForslag {
                førteTil shouldBe "Endring"
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett true
                }
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
            saksbehandler.leggTilOpplysning(
                Rettighetstype.skalEksportVurderes,
                true,
                "Drar til syden",
                gyldighetsperiode = Gyldighetsperiode(1.februar(2025)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            saksbehandler.lagBehandling(5.februar(2025))
            saksbehandler.leggTilOpplysning(
                Eksport.registrertIVertsland,
                true,
                "Har registrert seg i syden",
                gyldighetsperiode = Gyldighetsperiode(5.februar(2025)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = false)

            behandlingsresultatForslag(9) {
                person.avklaringer.map { it.kode } shouldContain "MeldekortMedEksport"
            }
            saksbehandler.lukkAlleAvklaringer().map { it.kode } shouldContain "MeldekortMedEksport"
            saksbehandler.godkjenn()

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Melder seg for periode 3 i tide, men svarer nei på fortsatt registrert
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(16).atTime(12, 21),
            )

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultatForslag(12) {
                førteTil shouldBe "Endring"
                opplysning(Meldeplikt.oppfyllerMeldeplikt) {
                    fra(1.januar(2025)) verdi true
                }
                opplysning(RegistrertArbeidssøker.registrertArbeidssøker) {
                    fra(1.januar(2025)) til 12.februar(2025) verdi true
                    fra(13.februar(2025)) verdi false
                }
                opplysning(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker) {
                    fra(1.januar(2025)) til 26.januar(2025) verdi true
                    fra(27.januar(2025)) til 12.februar(2025) verdi false
                    fra(13.februar(2025)) verdi false
                }
                rettighetsperioder {
                    fra(1.januar(2025)) til 31.januar(2025) harRett true
                    fra(1.februar(2025)) harRett true
                }
            }
        }
    }
}
