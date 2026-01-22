package no.nav.dagpenger.behandling.scenario

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeMonotonicallyIncreasing
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldEndWith
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldStartWith
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.helpers.scenario.assertions.Opplysningsperiode
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.behandling.mai
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.Opphold.oppholdINorge
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

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
            meldekortBatch(true)

            behandlingsresultatForslag {
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
                utbetalinger shouldHaveSize 11

                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 5036

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
            meldekortBatch()

            behandlingsresultatForslag {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 5036

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
            meldekortBatch(true)
            meldekortBatch(true)
            meldekortBatch(true)
            meldekortBatch(true)

            behandlingsresultat {
                utbetalinger.sumOf { it["utbetaling"].asInt() } shouldBe 42806

                // Bare de siste 14 dagene skal markeres som ny for de tilhører siste meldeperiode
                utbetalinger.count { it["opprinnelse"].asText() == "Ny" } shouldBe 14

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
            meldekortBatch(true)

            behandlingsresultatForslag {
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
            meldekortBatch()

            // Vi lager et forslag om beregning for hele meldeperioden
            behandlingsresultatForslag {
                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 28

                    // Første dag i ny meldeperiode
                    this.first { it.opprinnelse != Opplysningsperiode.Periodestatus.Arvet }.gyldigFraOgMed shouldBe 2.juli(2018)

                    // Siste dag i ny meldeperiode
                    this.last().gyldigFraOgMed shouldBe 15.juli(2018)

                    // Siste dag med forbruk
                    this.last { it.verdi.verdi == true }.gyldigFraOgMed shouldBe 13.juli(2018)
                }
            }

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

                with(opplysninger(Beregning.forbruk)) {
                    this shouldHaveSize 28

                    // Første dag i ny meldeperiode
                    this.first { it.opprinnelse != Opplysningsperiode.Periodestatus.Arvet }.gyldigFraOgMed shouldBe 2.juli(2018)

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
            meldekortBatch()

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat { rettighetsperioder.last().harRett shouldBe true }

            // Send inn meldekort
            person.sendInnMeldekort(Periode(25.juni(2018), 8.juli(2018)))

            // Systemet kjører beregningsbatchen
            meldekortBatch(true)
            meldekortBatch(true)
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
            meldekortBatch(true)

            // Verifiser at vi lager en avklaring om meldekort (så de ikke går automatisk i testfasen)
            person.avklaringer.first().kode shouldBe "MeldekortBehandling"

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
    fun `vi sperrer behandling av meldekort når de korrigerer en periode for langt bak i tid`() {
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
            meldekortBatch(true)
            meldekortBatch(true)
            meldekortBatch(true)

            // Send inn korrigering av forrige meldekort
            person.sendInnMeldekort(2, korrigeringAv = meldekortId, timer = List(14) { 7 })

            // Systemet kjører beregningsbatchen
            meldekortBatch()

            person.avklaringer.first().kode shouldBe "KorrigeringUtbetaltPeriode"

            // Avklaringen kan ikke lukkes av saksbehandler
            shouldThrow<IllegalArgumentException> {
                saksbehandler.lukkAlleAvklaringer()
            }
        }
    }

    @Test
    @Disabled("Denne testen avdekker en bug i meldekortprosessen der det er flere prøvingsdatoer i spill.")
    fun `beregning av flere meldekort der det også kommer en gjenopptak i mellom`() {
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

            // Opprett stans
            person.opprettBehandling(5.juli(2018))
            saksbehandler.endreOpplysning(oppholdINorge, false, "Er i utlandet", Gyldighetsperiode(5.juli(2018)))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()
            behandlingsresultat {
                rettighetsperioder.size shouldBe 2
            }

            // Send inn meldekort nr 1
            person.sendInnMeldekort(1)
            // Systemet kjører beregningsbatchen
            meldekortBatch(true)
            val meldekortId = person.sendInnMeldekort(2)
            // Send inn meldekort nr 2, men uten at den godkjennes enda
            meldekortBatch(false)

            // Gjenopptak mens vi venter på behandling av meldekort
            person.søkGjenopptak(20.juli(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Godkjenn meldekortene etter gjenopptak
            saksbehandler.lukkAlleAvklaringer(
                eksternHendelseId = meldekortId,
            )
            saksbehandler.godkjenn(
                eksternHendelseId = meldekortId,
            )

            behandlingsresultat {
                utbetalinger shouldHaveSize 14
            }

            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf(
                        // language=SQL
                        """
                        UPDATE meldekort SET behandling_startet = NULL, behandling_ferdig = NULL WHERE meldekort_id = :meldekortId;
                        """.trimIndent(),
                        mapOf(
                            "meldekortId" to meldekortId.toString(),
                        ),
                    ).asUpdate,
                )
            }

            meldekortBatch(true)

            behandlingsresultat {
                utbetalinger shouldHaveSize 14
            }
        }
    }

    @Disabled("Dette eksploderer fullstendig på grunn av utenErstattet() i Opplysninger")
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
            meldekortBatch(true)
            meldekortBatch(true)
            meldekortBatch(true)

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
            meldekortBatch()

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

                    // Nå er det jobbet over terskel og det skal ikke være noen forbruksdager
                    this.none { it.verdi.verdi == true } shouldBe true

                    // Siste dag i meldekort
                    this.last().gyldigFraOgMed shouldBe 1.juli(2018)
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
            meldekortBatch()

            // Verifiser gammel sats
            behandlingsresultatForslag {
                val satsPerDag = utbetalinger.map { it["sats"].asInt() }
                val utbetalingPerDag = utbetalinger.map { it["utbetaling"].asInt() }
                satsPerDag.shouldContainExactly(762, 762, 762, 762, 762, 762, 762, 762, 762, 762, 762)
                utbetalingPerDag.shouldContainExactly(435, 435, 0, 0, 435, 435, 435, 435, 438, 0, 0)
            }

            // Endre barnetillegg midt i meldekortbehandlingen
            saksbehandler.endreOpplysning(
                DagpengenesStørrelse.barnetilleggetsStørrelse,
                Beløp(380.0),
                "",
                Gyldighetsperiode(25.juni(2018)),
            )

            // Verifiser at vi får ny og høyere sats fra og med 25. juni
            behandlingsresultatForslag {
                val satsPerDag = utbetalinger.map { it["sats"].asInt() }
                val utbetalingPerDag = utbetalinger.map { it["utbetaling"].asInt() }
                satsPerDag.shouldContainExactly(762, 762, 762, 762, 1074, 1074, 1074, 1074, 1074, 1074, 1074)
                utbetalingPerDag.shouldContainExactly(509, 510, 0, 0, 717, 717, 717, 717, 721, 0, 0)
            }
        }
    }
}
