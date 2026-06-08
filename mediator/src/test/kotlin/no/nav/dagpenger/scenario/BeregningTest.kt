package no.nav.dagpenger.scenario

import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeMonotonicallyIncreasing
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldEndWith
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldStartWith
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.mediator.mai
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.scenario.assertions.Opplysningsperiode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.math.round

class BeregningTest {
    @Test
    fun `beregning av et meldekort`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe true
            }

            // Send inn meldekort
            person.sendInnMeldekort(1)

            // Systemet kjører beregningsbatchen
            meldekortBatch(markerFerdig = true)

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe true

                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 14

                    // Første dag i meldekort
                    this.first().gyldigFraOgMed shouldBe 18.juni(2018)

                    // Første forbruksdag
                    this.first { it.verdi.verdi == true }.gyldigFraOgMed shouldBe 21.juni(2018)

                    // Siste dag i meldekort
                    this.last().gyldigFraOgMed shouldBe 1.juli(2018)
                }

                // Første forbruksdag er 21, så 11 dager i perioden gir utbetaling
                utbetalinger.toList() shouldHaveSize 11

                utbetalinger.toList().sumOf { it["utbetaling"].asInt() } shouldBe 5036

                with(opplysninger(Beregning.forbrukt)) {
                    none { it.opprinnelse == Opplysningsperiode.Periodestatus.Arvet } shouldBe true
                    map { it.verdi.verdi }.shouldContainExactly(0, 0, 0, 1, 2, 2, 2, 3, 4, 5, 6, 7, 7, 7)
                    map { it.gyldigFraOgMed.toString() }.shouldContainExactly(
                        "2018-06-18",
                        "2018-06-19",
                        "2018-06-20",
                        "2018-06-21",
                        "2018-06-22",
                        "2018-06-23",
                        "2018-06-24",
                        "2018-06-25",
                        "2018-06-26",
                        "2018-06-27",
                        "2018-06-28",
                        "2018-06-29",
                        "2018-06-30",
                        "2018-07-01",
                    )
                }
            }

            // Send inn meldekort
            person.sendInnMeldekort(1)

            // Systemet kjører beregningsbatchen
            meldekortBatch(markerFerdig = true)

            behandlingsresultat {
                utbetalinger.toList().sumOf { it["utbetaling"].asInt() } shouldBe 5036

                with(opplysninger(Beregning.forbrukt)) {
                    forAll { it.opprinnelse shouldBe Opplysningsperiode.Periodestatus.Ny }

                    map { it.verdi.verdi }.shouldContainExactly(0, 0, 0, 1, 2, 2, 2, 3, 4, 5, 6, 7, 7, 7)
                    map { it.gyldigFraOgMed.toString() }.shouldContainExactly(
                        "2018-06-18",
                        "2018-06-19",
                        "2018-06-20",
                        "2018-06-21",
                        "2018-06-22",
                        "2018-06-23",
                        "2018-06-24",
                        "2018-06-25",
                        "2018-06-26",
                        "2018-06-27",
                        "2018-06-28",
                        "2018-06-29",
                        "2018-06-30",
                        "2018-07-01",
                    )
                }
            }
        }
    }

    @Test
    fun `beregning av flere meldekort etter hverandre`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe true
            }

            // Send inn meldekort
            person.sendInnMeldekort(1)
            person.sendInnMeldekort(2)
            person.sendInnMeldekort(3)
            person.sendInnMeldekort(4)

            // Systemet kjører beregningsbatchen
            meldekortBatch(markerFerdig = true)
            meldekortBatch(markerFerdig = true)
            meldekortBatch(markerFerdig = true)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat {
                utbetalinger.toList().sumOf { it["utbetaling"].asInt() } shouldBe 42806

                // Bare de siste 14 dagene skal markeres som ny for de tilhører siste meldeperiode
                utbetalinger.toList().count { it["opprinnelse"].asString() == "Ny" } shouldBe 14

                with(opplysninger(Beregning.forbrukt)) {
                    val forbruksdager = map { it.verdi.verdi as Int }
                    forbruksdager.shouldBeMonotonicallyIncreasing()

                    forbruksdager.shouldStartWith(0)
                    forbruksdager.shouldEndWith(37)
                }
            }
        }
    }

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

            behandlingsresultat { rettighetsperioder.last().harRett shouldBe true }

            // Send inn meldekort
            person.sendInnMeldekort(1)
            person.sendInnMeldekort(2)

            // Systemet kjører beregningsbatchen
            meldekortBatch(markerFerdig = true)

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
            }

            // Systemet kjører beregningsbatchen
            meldekortBatch(markerFerdig = true)

            // Vi lager et forslag om beregning for hele meldeperioden
            behandlingsresultat {
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 28
                    this.count { it.verdi.verdi == true } shouldBe 17

                    // Første dag i ny meldeperiode
                    this.first { it.opprinnelse != Opplysningsperiode.Periodestatus.Arvet }.gyldigFraOgMed shouldBe 2.juli(2018)

                    // Siste dag i ny meldeperiode
                    this.last().gyldigFraOgMed shouldBe 15.juli(2018)

                    // Siste dag med forbruk
                    this.last { it.verdi.verdi == true }.gyldigFraOgMed shouldBe 13.juli(2018)
                }
            }

            saksbehandler.omgjørBehandling(6.juli(2018))

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
                with(opplysninger(Opphold.oppfyllerKravetTilOpphold)) {
                    this shouldHaveSize 2
                    this.first().gyldigFraOgMed shouldBe 21.juni(2018)
                    this.first().verdi.verdi shouldBe true
                    this.last().gyldigFraOgMed shouldBe 8.juli(2018)
                    this.last().verdi.verdi shouldBe false
                }

                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 28
                    this.count { it.verdi.verdi == true } shouldBeLessThan 17
                    this.count { it.verdi.verdi == true } shouldBe 12 // Dagene på slutten er ikke forbruksdager lengre

                    // Første dag i ny meldeperiode
                    this.first().gyldigFraOgMed shouldBe 18.juni(2018)

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
            meldekortBatch(markerFerdig = true)

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat { rettighetsperioder.last().harRett shouldBe true }

            // Send inn meldekort
            person.sendInnMeldekort(Periode(25.juni(2018), 8.juli(2018)))

            // Systemet kjører beregningsbatchen
            meldekortBatch(markerFerdig = true)
            meldekortBatch(markerFerdig = true)
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

            behandlingsresultat { rettighetsperioder.last().harRett shouldBe true }

            // Send inn meldekort
            val meldekortId = person.sendInnMeldekort(1)

            // Systemet kjører beregningsbatchen
            meldekortBatch(markerFerdig = true)

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

                with(opplysninger(Beregning.gjenståendeEgenandel)) {
                    this shouldHaveSize 1
                    first().verdi.verdi shouldBe 0
                }
                with(opplysninger(Beregning.forbruktEgenandel)) {
                    this shouldHaveSize 1
                    first().verdi.verdi shouldBe 3777
                }
                with(opplysninger(Beregning.oppfyllerKravTilTaptArbeidstidIPerioden)) {
                    this shouldHaveSize 1
                    first().verdi.verdi shouldBe true
                }
            }

            // Send inn korrigering av forrige meldekort
            person.sendInnMeldekort(1, korrigeringAv = meldekortId, timer = List(14) { 7 })

            // Systemet kjører beregningsbatchen
            meldekortBatch()

            // Verifiser at vi lager en avklaring om korrigert meldekort
            person.avklaringer.first().kode shouldBe "KorrigertMeldekortBehandling"

            // Vi lager et forslag om reberegning av forrige periode
            behandlingsresultatForslag {
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 14

                    // Ingen opplysninger om forbruk skal være arvet
                    this.none { it.opprinnelse == Opplysningsperiode.Periodestatus.Arvet } shouldBe true

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
                with(opplysninger(Beregning.oppfyllerKravTilTaptArbeidstidIPerioden)) {
                    this shouldHaveSize 1
                    // Jobber 7 timer hver dag og vil være over terskel
                    first().verdi.verdi shouldBe false
                }
            }
        }
    }

    @Test
    fun `vi kan reberegne meldekort når de korrigeres (tidligere periode)`() {
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
            meldekortBatch(markerFerdig = true)

            person.avklaringer.first().kode shouldBe "KorrigeringUtbetaltPeriode"

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
                    this.none { it.opprinnelse == Opplysningsperiode.Periodestatus.Arvet } shouldBe true

                    // Første dag i ny meldeperiode
                    this.first().gyldigFraOgMed shouldBe 18.juni(2018)

                    // Siste dag i meldekort
                    this.last().gyldigFraOgMed shouldBe 29.juli(2018)
                }
            }
        }
    }

    @Test
    fun `vi kan håndtere endring av barnetillegg og sats midt i meldekort`() {
        nyttScenario {
            inntektSiste12Mnd = 300000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Send inn meldekort
            person.sendInnMeldekort(1)

            // Systemet kjører beregningsbatchen
            meldekortBatch(markerFerdig = true)

            // Verifiser gammel sats
            behandlingsresultat {
                val satsPerDag = utbetalinger.toList().map { it["sats"].asInt() }
                val utbetalingPerDag = utbetalinger.toList().map { it["utbetaling"].asInt() }
                satsPerDag.shouldContainExactly(762, 762, 762, 762, 762, 762, 762, 762, 762, 762, 762)
                utbetalingPerDag.shouldContainExactly(435, 435, 0, 0, 435, 435, 435, 435, 438, 0, 0)
            }

            saksbehandler.omgjørBehandling(22.juni(2018))
            // Endre barnetillegg midt i meldekortbehandlingen
            saksbehandler.endreOpplysning(
                DagpengenesStørrelse.barnetilleggetsStørrelse,
                Beløp(380.0),
                "",
                Gyldighetsperiode(25.juni(2018)),
            )

            // Verifiser at vi får ny og høyere sats fra og med 25. juni
            behandlingsresultatForslag {
                val satsPerDag = utbetalinger.toList().map { it["sats"].asInt() }
                val utbetalingPerDag = utbetalinger.toList().map { it["utbetaling"].asInt() }
                satsPerDag.shouldContainExactly(762, 762, 762, 762, 1074, 1074, 1074, 1074, 1074, 1074, 1074)
                utbetalingPerDag.shouldContainExactly(509, 510, 0, 0, 717, 717, 717, 717, 721, 0, 0)
            }
        }
    }

    @Test
    fun `arbeid over terskel 3 meldekort på rad`() {
        nyttScenario {
            inntektSiste12Mnd = 300000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Send inn meldekort
            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            // Send inn meldekort hvor en har jobbet for mye
            person.sendInnMeldekort(2, timer = List(14) { 7 })
            meldekortBatch(markerFerdig = true)

            // Send inn meldekort hvor en har jobbet for mye
            person.sendInnMeldekort(3, timer = List(14) { 7 })
            meldekortBatch(markerFerdig = true)

            // Send inn meldekort hvor en har jobbet for mye
            person.sendInnMeldekort(4, timer = List(14) { 7 })
            meldekortBatch(markerFerdig = true)

            behandlingsresultat {
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 2.juli(2018)

                opplysninger(RegistrertArbeidssøker.registrertArbeidssøker).last().verdi.verdi shouldBe true
            }

            // Verifiser at vi kan håndtere melding om avsluttet arbeidssøkerperiode etter stans
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = 4.juli(2018),
                avsluttetTidspunkt = 4.juli(2018).atTime(11, 21),
            )

            // Lukker avklaringen som tvinger alle stans til manuell behandling
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 2.juli(2018)

                opplysninger(RegistrertArbeidssøker.registrertArbeidssøker).last().verdi.verdi shouldBe false
            }
        }
    }

    // Dette er et behov for POPP i framtida. De trenger å vite hvor mye som er utbetalt som dagpenger ekslusive barnetillegg
    @Test
    @Disabled("Bare en POC for hvordan dette kan gjøres")
    fun `beregning av hvor mye barnetillegg utgjør av utbetalt`() {
        nyttScenario {
            inntektSiste12Mnd = 300000
        }.test {
            person.søkDagpenger(18.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Send inn meldekort
            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            person.sendInnMeldekort(2, timer = List(14) { 2 })
            meldekortBatch(markerFerdig = true)
            person.sendInnMeldekort(3, timer = List(14) { 2 })
            meldekortBatch(markerFerdig = true)
            person.sendInnMeldekort(4, timer = List(14) { 2 })
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(5) {
                val bruttoUtbetalt = opplysninger(Beregning.utbetalingForPeriode).sumOf { it.verdi.verdi as Int }
                bruttoUtbetalt shouldBe 19659

                val alleBarnetillegg = opplysninger(DagpengenesStørrelse.barnetillegg)
                val snittBarnetillegg = alleBarnetillegg.sumOf { it.verdi.verdi as Int } / alleBarnetillegg.size
                snittBarnetillegg shouldBe 17

                val dagerMedForbruk = opplysninger(Beregning.forbruk).count { it.verdi.verdi == true }
                val totalBarnetillegg = dagerMedForbruk * snittBarnetillegg
                totalBarnetillegg shouldBe 680

                val gradering = opplysninger(Beregning.prosentfaktor)
                val snittGradering = gradering.sumOf { it.verdi.verdi as Double } / gradering.size
                snittGradering shouldBe 0.7200000000000002

                val andelBarnetillegg = round(totalBarnetillegg * snittGradering)
                andelBarnetillegg shouldBe 490
            }
        }
    }
}
