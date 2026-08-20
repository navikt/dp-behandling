package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.opplysning.Avgjørelse.Endring
import no.nav.dagpenger.opplysning.Avgjørelse.Stans
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting.gjenståendePermittering
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting.innenforFritaksperioden
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlageValg.skalOmgjøringUtenKlageVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering.oppfyllerKravetTilPermittering
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalPermitteringVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntekt
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.scenario.assertions.Opplysningsperiode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PermitteringTest {
    @Test
    fun `tester innvilgelse ved permittering og beregning av forbruk`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch()

            behandlingsresultat(2) {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 7
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            person.sendInnMeldekort(2)
            meldekortBatch()

            behandlingsresultat(3) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 17
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 113
                }
            }
        }
    }

    @Test
    fun `tester pause av permittering og beregning av forbruk`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch()

            behandlingsresultat(2) {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 7
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            // Stanser permittering og forbruker bare ordinær
            saksbehandler.lagBehandling(2.juli(2018))
            saksbehandler.endreOpplysning(
                skalPermitteringVurderes,
                false,
                "Permitteringen er satt på pause",
                Gyldighetsperiode(2.juli(2018)),
            )
            saksbehandler.endreOpplysning(
                oppfyllerKravetTilPermittering,
                false,
                "Permitteringen er satt på pause",
                Gyldighetsperiode(2.juli(2018)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(3) {
                with(opplysninger(oppfyllerKravetTilPermittering)) {
                    this.last().verdi.verdi shouldBe false
                }
            }

            // Sender meldekort som ikke-permittert
            person.sendInnMeldekort(2)
            meldekortBatch()

            behandlingsresultat(4) {
                with(opplysninger(oppfyllerKravetTilPermittering)) {
                    this.last().verdi.verdi shouldBe false
                }
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 7
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            // Blir permittert igjen
            saksbehandler.lagBehandling(16.juli(2018))
            saksbehandler.endreOpplysning(
                skalPermitteringVurderes,
                true,
                "Permitteringen er startet igjen",
                Gyldighetsperiode(16.juli(2018)),
            )
            saksbehandler.endreOpplysning(
                oppfyllerKravetTilPermittering,
                true,
                "Permitteringen er startet igjen",
                Gyldighetsperiode(16.juli(2018)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(5) {}

            person.sendInnMeldekort(3)
            meldekortBatch()

            behandlingsresultat(6) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 17
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 113
                }
            }
        }
    }

    @Test
    fun `nedjustering av tildelingsgrunnlag med senere virkningsdato skal ikke telles om igjen for allerede prosesserte meldeperioder`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                utfall shouldBe true

                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
            }

            // Meldeperiode 1: forbruker 7 av 130 dager (26 uker) med kapasitet 26 uker.
            person.sendInnMeldekort(1)
            meldekortBatch()

            behandlingsresultat(2) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 7
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            // Meldeperiode 2: forbruker totalt 17 av 130 dager, fortsatt med kapasitet 26 uker.
            person.sendInnMeldekort(2)
            meldekortBatch()

            behandlingsresultat(3) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 17
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 113
                }
            }

            // Saksbehandler omgjør behandlingen og nedjusterer tildelingsgrunnlaget fra 26 til 20 uker,
            // men KUN med virkning fra 16. juli - altså *etter* at meldeperiode 1 og 2 allerede er
            // ferdig behandlet med kapasitet 26 uker (130 dager).
            saksbehandler.omgjørBehandling(21.juni(2018))
            saksbehandler.endreOpplysning(
                PermitteringFastsetting.permitteringsperiode,
                20,
                "Nedjustering av permitteringsperiode, gjelder fra 16. juli",
                Gyldighetsperiode(16.juli(2018)),
            )
            saksbehandler.endreOpplysning(
                skalOmgjøringUtenKlageVurderes,
                true,
                "Test",
                Gyldighetsperiode(21.juni(2018)),
            )
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(
                OmgjøringUtenKlage.ansesUgyldigVedtak,
                true,
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(4) {
                // Meldeperiode 1 og 2 blir beregnet på nytt av OmgjøringBeregningPlugin, selv om
                // nedjusteringen først skal gjelde fra 16. juli.
                //
                // Siden 16. juli ligger *etter* meldeperiode 1 og 2, skal disse periodenes
                // forbrukt/gjenstående forbli uendret (kapasitet 26 uker/130 dager var fortsatt
                // gjeldende da disse dagene faktisk ble forbrukt). Dette sikres av at
                // KvoteDefinisjon.tildeltKapasitet(...) nå slår opp kapasiteten på den datoen som
                // faktisk beregnes, i stedet for alltid å hente den nyeste/gjeldende verdien.
                val forbruktPerioder = opplysninger(PermitteringFastsetting.forbruktPermittering)
                val gjenståendePerioder = opplysninger(gjenståendePermittering)

                fun List<Opplysningsperiode>.gjeldendeFor(dato: LocalDate) =
                    single { it.gyldigFraOgMed!! <= dato && (it.gyldigTilOgMed == null || it.gyldigTilOgMed >= dato) }.verdi.verdi as Int

                forbruktPerioder.gjeldendeFor(1.juli(2018)) shouldBe 7
                forbruktPerioder.gjeldendeFor(15.juli(2018)) shouldBe 17

                // Gjenstående for meldeperiode 1 og 2 skal fortsatt være 123 og 113 (basert på
                // kapasitet 130 dager, som fortsatt gjaldt på det tidspunktet), IKKE 93/83 (basert
                // på ny kapasitet 100 dager som først skal gjelde fra 16. juli).
                gjenståendePerioder.gjeldendeFor(1.juli(2018)) shouldBe 123
                gjenståendePerioder.gjeldendeFor(15.juli(2018)) shouldBe 113
            }

            // Sender et nytt meldekort etter virkningsdatoen for nedjusteringen, og sjekker at
            // den nye (lavere) kapasiteten på 100 dager brukes for dette meldekortet. Siden
            // nedjusteringen er en NY tildeling (ikke bare et nytt kapasitetstak), starter
            // tellingen på nytt fra 0 fra og med virkningsdatoen - forbruk fra FØR 16. juli
            // (17 dager) telles altså ikke med mot den nye kapasiteten.
            person.sendInnMeldekort(3)
            meldekortBatch()

            behandlingsresultat(5) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 10
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 90
                }
            }
        }
    }

    @Test
    fun `nedjustering av tildelingsgrunnlag i vanlig revurdering skal telle riktig for nye meldeperioder`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
            }

            // Meldeperiode 1: forbruker 7 av 130 dager (26 uker).
            person.sendInnMeldekort(1)
            meldekortBatch()

            behandlingsresultat(2) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 7
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            // Vanlig revurdering (ikke omgjøring): saksbehandler nedjusterer tildelingsgrunnlaget
            // fra 26 til 20 uker, med virkning fra 16. juli - altså *etter* meldeperiode 1.
            saksbehandler.lagBehandling(16.juli(2018))
            saksbehandler.endreOpplysning(
                PermitteringFastsetting.permitteringsperiode,
                20,
                "Nedjustering av permitteringsperiode, gjelder fra 16. juli",
                Gyldighetsperiode(16.juli(2018)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(3) {
                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.last().verdi.verdi shouldBe 20
                }
                // Meldeperiode 1 (allerede prosessert) skal fortsatt vise 123 gjenstående -
                // en vanlig revurdering skal ikke regne om gamle meldeperioder.
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            // Meldeperiode 2 (2.-15. juli) starter fortsatt *før* virkningsdatoen (16. juli), og
            // skal derfor telle videre på gammel kapasitet (130 dager): 7 + 10 = 17 forbrukt,
            // 130 - 17 = 113 gjenstående.
            person.sendInnMeldekort(2)
            meldekortBatch()

            behandlingsresultat(4) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 17
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 113
                }
            }

            // Meldeperiode 3 (16.-29. juli) starter *på* virkningsdatoen for nedjusteringen. En
            // nedjustering er en NY tildeling, ikke bare et nytt kapasitetstak - forbruket telles
            // derfor på nytt fra 0 fra og med 16. juli: 10 nye dager forbrukt mot 100 dager (20
            // uker) ny kapasitet, altså 100 - 10 = 90 gjenstående. Forbruket fra periode 1 og 2
            // (17 dager, talt mot den gamle kapasiteten på 130 dager) telles ikke med.
            person.sendInnMeldekort(3)
            meldekortBatch()

            behandlingsresultat(5) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 10
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 90
                }
            }
        }
    }

    @Test
    fun `nedjustering av tildelingsgrunnlag midt i en løpende meldeperiode skal telle riktig fra virkningsdato`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                with(opplysninger(PermitteringFastsetting.permitteringsperiode)) {
                    this.single().verdi.verdi shouldBe 26
                }
            }

            // Meldeperiode 1 (21. juni - 4. juli): forbruker 7 av 130 dager (26 uker).
            person.sendInnMeldekort(1)
            meldekortBatch()

            behandlingsresultat(2) {
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 123
                }
            }

            // Vanlig revurdering: saksbehandler nedjusterer tildelingsgrunnlaget fra 26 til 20
            // uker, med virkning fra 9. juli - altså *midt i* meldeperiode 2 (2.-15. juli).
            saksbehandler.lagBehandling(9.juli(2018))
            saksbehandler.endreOpplysning(
                PermitteringFastsetting.permitteringsperiode,
                20,
                "Nedjustering av permitteringsperiode, gjelder fra 9. juli",
                Gyldighetsperiode(9.juli(2018)),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Meldeperiode 2 (2.-15. juli) starter *før* virkningsdatoen (9. juli). Siden
            // nedjusteringen er en NY tildeling, ikke bare et nytt kapasitetstak, nullstilles
            // telleren fra og med 9. juli - kun permitteringsdagene fra 9. til 15. juli telles
            // mot den nye kapasiteten på 100 dager (20 uker). Forbruket fra FØR 9. juli (både
            // periode 1 og starten av periode 2) telles ikke med mot den nye tildelingen.
            person.sendInnMeldekort(2)
            meldekortBatch()

            behandlingsresultat(4) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 5
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 95
                }
            }
        }
    }

    @Test
    fun `teller riktige dager ved permittering`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(18.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) { }

            person.sendInnMeldekort(
                1,
                aktiviteter =
                    listOf(
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Fravær,
                        MeldekortAktivitet.Syk,
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                        MeldekortAktivitet.Arbeid(0),
                    ),
            )
            meldekortBatch()

            behandlingsresultat(2) {
                with(opplysninger(PermitteringFastsetting.forbruktPermittering)) {
                    this.last().verdi.verdi shouldBe 8
                }
                with(opplysninger(gjenståendePermittering)) {
                    this.last().verdi.verdi shouldBe 122
                }
            }
        }
    }

    @Test
    fun `stanser rett til dagpenger når fritaksperioden er oppbrukt`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(19.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(PermitteringFastsetting.permitteringsperiode, 3, "Redusert permitteringsperiode")
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) { }

            person.sendInnMeldekort(1)
            meldekortBatch()
            behandlingsresultat(2) {
                førteTil shouldBe Endring.toString()
                opplysninger(gjenståendePermittering).last().verdi.verdi shouldBe 6
            }

            person.sendInnMeldekort(2)
            meldekortBatch()
            behandlingsresultat(3) {
                førteTil shouldBe Stans.toString()
                opplysninger(gjenståendePermittering).last().verdi.verdi shouldBe 0

                opplysninger(oppfyllerKravetTilPermittering) {
                    shouldHaveSize(2)
                    first().verdi.verdi shouldBe true
                    last().verdi.verdi shouldBe false
                }

                opplysninger(innenforFritaksperioden) {
                    shouldHaveSize(2)
                    last().verdi.verdi shouldBe false
                }

                opplysninger(kravTilTapAvArbeidsinntekt).last().verdi.verdi shouldBe false

                rettighetsperioder.first { !it.harRett }.fraOgMed shouldBe 10.juli(2018)

                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 14904
            }
        }
    }
}
