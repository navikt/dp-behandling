package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode.antallStønadsdager
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class OpphørTest {
    @Test
    fun `vi lager opphør når ordinær dagpengeperiode er oppbrukt`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(18.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(antallStønadsdager, 13)
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                førteTil shouldBe Avgjørelse.Innvilgelse.toString()
                rettighetsperioder shouldHaveSize 1
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(2) {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBeGreaterThan 0
                utbetalinger.count { it["utbetaling"].asInt() > 0 } shouldBe 10
            }
            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(3) {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBeGreaterThan 0
                utbetalinger.count { it["utbetaling"].asInt() > 0 } shouldBe 13
                førteTil shouldBe Avgjørelse.Opphør.toString()
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe false
                rettighetsperioder.last().fraOgMed shouldBe 5.juli(2018)
            }

            // En ekstra rekjøringsrunde (nytt meldekort) etter at kvoten allerede er brukt opp -
            // her testes om flippet overlever en ny regelkjøring, ikke bare den som skrev det.
            person.sendInnMeldekort(3)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(4) {
                rettighetsperioder.last().harRett shouldBe false
            }
        }
    }

    @Test
    fun `tester at stans etter oppbrukt periode overlever en ekstra rekjøring`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(antallStønadsdager, 5)

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder shouldHaveSize 1
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(2) {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe false
            }

            // En ekstra rekjøringsrunde (nytt meldekort) etter at kvoten allerede er brukt opp -
            // her testes om flippet overlever en ny regelkjøring, ikke bare den som skrev det.
            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(3) {
                rettighetsperioder.last().harRett shouldBe false
            }
        }
    }

    @Test
    fun `vi kan revurdere opphør`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(antallStønadsdager, 15)
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder shouldHaveSize 1
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            // Meldekort 2
            val meldekortUUID = person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(3) {
                førteTil shouldBe Avgjørelse.Opphør.toString()
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe false
            }

            person.sendInnMeldekort(
                2,
                korrigeringAv = meldekortUUID,
                aktiviteter =
                    listOf(
                        MeldekortAktivitet.Arbeid(8),
                        MeldekortAktivitet.Arbeid(8),
                        MeldekortAktivitet.Arbeid(8),
                        MeldekortAktivitet.Arbeid(8),
                        MeldekortAktivitet.Arbeid(8),
                    ),
            )
            meldekortBatch(markerFerdig = false)
            person.avklaringer.map { it.kode } shouldContain "KorrigeringUtbetaltPeriode"

            behandlingsresultatForslag(4) {
                // TODO: Kanskje Endring er bedre?
                førteTil shouldBe Avgjørelse.Gjenopptak.toString()
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.last().harRett shouldBe true
            }
        }
    }

    // BUG: harLøpendeRett fryses permanent til usann fra den datoen kvoten FØRST ble brukt opp
    // (KvotetellingsSkriver.settUtløsendeBetingelseTilUsann), og RettighetsperiodePlugin nekter
    // (harPilla) å noensinne skrive en ny/utvidet rettighetsperiode etter dette. Når man så korrigerer
    // meldekortet med lavere arbeidstimer (som fortsatt oppfyller kravet til tapt arbeidstid, og derfor
    // fortsatt SKAL konsumere dager), blir "dagerMedRett"-vinduet likevel begrenset til den gamle,
    // frosne grensen (til og med dagen FØR frysingen inntraff) - ikke det korrigerte, fulle vinduet.
    // Resultatet er at færre dager enn budsjettert klassifiseres som Arbeidsdag, kvoten telles aldri
    // helt tom på nytt, og korrigeringen ender feilaktig med Stans i stedet for et opprettholdt Opphør.
    // Dette krever trolig at harLøpendeRett kan re-utledes friskt (ikke bare vinduet for
    // tapt-arbeidstid-beregningen isolert) når en korrigering endrer forbruket for en allerede
    // ferdigbehandlet periode.
    @Disabled("Kjent bug: harLøpendeRett sin frosne grense oppdateres ikke ved korrigering, se kommentar over")
    @Test
    fun `vi kan revurdere opphør men arbeid under terskel opprettholder opphøret`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            saksbehandler.endreOpplysning(antallStønadsdager, 15)
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder shouldHaveSize 1
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            // Meldekort 2
            val meldekortUUID = person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(3) {
                førteTil shouldBe Avgjørelse.Opphør.toString()
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe false
            }

            // Arbeidstimene her ligger UNDER kravet til tapt arbeidstid (35t av 75t = 46,7 % <= 50 %-
            // terskelen), slik at kravet fortsatt er oppfylt og dagene telles med som forbruk - i
            // motsetning til testen over. Da er den ordinære kvoten (15 dager) faktisk brukt opp, og
            // korrigeringen skal derfor IKKE gjenopprette retten - opphøret skal stå seg.
            person.sendInnMeldekort(
                2,
                korrigeringAv = meldekortUUID,
                aktiviteter =
                    listOf(
                        MeldekortAktivitet.Arbeid(7),
                        MeldekortAktivitet.Arbeid(7),
                        MeldekortAktivitet.Arbeid(7),
                        MeldekortAktivitet.Arbeid(7),
                        MeldekortAktivitet.Arbeid(7),
                    ),
            )
            meldekortBatch(markerFerdig = false)

            behandlingsresultatForslag(3) {
                førteTil shouldBe Avgjørelse.Opphør.toString()
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder.last().harRett shouldBe false
            }
        }
    }
}
