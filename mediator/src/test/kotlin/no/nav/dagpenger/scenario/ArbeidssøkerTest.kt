package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.mediator.api.models.OpprinnelseDTO
import no.nav.dagpenger.mediator.asUUID
import no.nav.dagpenger.mediator.august
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse.Companion.behandlingFlyttetAvklaring
import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.Meldeplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.kravPåLønn
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.scenario.assertions.BehandlingsresultatAssertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class ArbeidssøkerTest {
    // Scenario 2
    @Test
    fun `stanser fordi bruker blir avregistrert i ASR`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder {
                    fra(21.juni(2018)) harRett true
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Blir avregistrert av veileder første torsdag i meldeperiode 3
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(3).atTime(12, 21),
                manueltAvregistrert = true,
            )
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag(3) {
                førteTil shouldBe Avgjørelse.Stans.toString()

                with(opplysninger(Meldeplikt.oppfyllerMeldeplikt)) {
                    this shouldHaveSize 1
                }
                opplysning(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker) {
                    fra(21.juni(2018)) til 19.juli(2018) verdi true
                    fra(20.juli(2018)) verdi false
                }
                // Stansen skal være fra og med torsdag
                rettighetsperioder {
                    fra(21.juni(2018)) til 19.juli(2018) harRett true
                    fra(20.juli(2018)) harRett false
                }
                rettighetsperioder.map { it.opprinnelse } shouldBe listOf(OpprinnelseDTO.ARVET, OpprinnelseDTO.NY)
            }
        }
    }

    // Scenario 3
    @Test
    fun `stanser fordi bruker ikke har meldt seg innen 21-dagers frist`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder {
                    fra(21.juni(2018)) harRett true
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Blir avregistrert etter 21 dager uten å ha meldt seg
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(16).atTime(12, 21),
                fristBrutt = true,
            )

            // Lukker avklaringen som tvinger alle stans til manuell behandling
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat(4) {
                opplysning(Meldeplikt.oppfyllerMeldeplikt) {
                    fra(21.juni(2018)) til 15.juli(2018) verdi true
                    fra(16.juli(2018)) verdi false
                }
                opplysning(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker) {
                    fra(21.juni(2018)) til 1.august(2018) verdi true
                    fra(2.august(2018)) verdi false
                }

                rettighetsperioder {
                    fra(21.juni(2018)) til 15.juli(2018) harRett true
                    fra(16.juli(2018)) harRett false
                }

                førteTil shouldBe Avgjørelse.Stans.toString()
            }
        }
    }

    // Scenario 4
    @Test
    fun `stanser fordi bruker sier nei på meldekort`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(21.juni(2018)) harRett true
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(2) {
                opplysninger(Meldeplikt.oppfyllerMeldeplikt) {
                    this shouldHaveSize 1
                }
            }

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(3) {
                opplysninger(Meldeplikt.oppfyllerMeldeplikt) {
                    this shouldHaveSize 1
                }
            }

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Melder seg for periode 3 i tide, men nei som videre registrert
            // Meldedato er 16. juli
            // Siste registrerte dag er 1. august, ikke registrert fra 2. august
            // Stans skal gjelde fra 16. juli
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(16).atTime(12, 21),
            )

            // Lukker avklaringen som tvinger alle stans til manuell behandling
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat(nummer = 4) {
                førteTil shouldBe Avgjørelse.Stans.toString()

                with(opplysninger(Meldeplikt.oppfyllerMeldeplikt)) {
                    this shouldHaveSize 1
                }
                with(opplysninger(RegistrertArbeidssøker.registrertArbeidssøker)) {
                    this shouldHaveSize 2
                }
                opplysning(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker) {
                    fra(21.juni(2018)) til 15.juli(2018) verdi true
                    fra(16.juli(2018)) til 1.august(2018) verdi false
                    fra(2.august(2018)) verdi false
                }

                rettighetsperioder {
                    fra(21.juni(2018)) til 15.juli(2018) harRett true
                    fra(16.juli(2018)) harRett false
                }
            }
        }
    }

    @Test
    fun `blir avregistrert i ASR og sender meldekort`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(21.juni(2018)) harRett true
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(2) {}

            person.sendInnMeldekort(2)
            meldekortBatch(markerFerdig = true)
            behandlingsresultat(3) {}

            person.sendInnMeldekort(3)
            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Sier nei på meldekort
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(3).atTime(12, 21),
            )
            behovsløsere.løsTilForslag()

            var stansId: UUID? = null
            behandlingsresultatForslag(3) {
                behandletHendelse["type"].asString() shouldBe "Arbeidssøkerperiode"
                stansId = behandletHendelse["id"].asUUID()
            }
            saksbehandler.lukkAlleAvklaringer(stansId!!)

            meldekortBatch(markerFerdig = true)
            var sisteForbrukteDag: LocalDate? = null
            behandlingsresultat(4) {
                behandletHendelse["type"].asString() shouldBe "Meldekort"
                sisteForbrukteDag = opplysninger(Beregning.forbruk).last().gyldigFraOgMed
            }

            // Simulerer at vi tar imot flytt_behandling og flytter søsken (dette løses automatisk av behovsløsere)
            val flytt = rapidInspektør.sisteMelding("flytt_behandling").second
            saksbehandler.flyttBehandlingTilNyKjede(flytt["behandlingId"].asUUID(), flytt["nyBasertPåId"].asUUID())

            // Sjekk at behandlingene som godkjennes får en avklaring som viser at det har skjedd
            saksbehandler.lukkAlleAvklaringer(stansId) shouldContain behandlingFlyttetAvklaring

            saksbehandler.godkjenn(stansId)
            behandlingsresultat(5) {
                behandletHendelse["type"].asString() shouldBe "Arbeidssøkerperiode"

                opplysninger(Beregning.forbruk).last().gyldigFraOgMed shouldBe sisteForbrukteDag
            }
        }
    }

    @Test
    fun `oppretter behandling uten avklaring etter avsluttet arbeidssøkerperiode når søknaden allerede har stanset`() {
        nyttScenario {
            inntektSiste12Mnd = 300000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            val stansdato = person.fastsattMeldedato(4).plusDays(7)
            saksbehandler.leggTilOpplysning(
                Opphold.oppholdINorge,
                true,
                "Oppholder seg i Norge",
                Gyldighetsperiode(21.juni(2018), stansdato.minusDays(1)),
            )
            saksbehandler.leggTilOpplysning(
                Opphold.oppholdINorge,
                false,
                "Oppholder seg i utlandet",
                Gyldighetsperiode(stansdato),
            )
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder {
                    fra(21.juni(2018)) til stansdato.minusDays(1) harRett true
                    fra(stansdato) harRett false
                }
            }

            repeat(3) { index ->
                person.sendInnMeldekort(index + 1)
                meldekortBatch(markerFerdig = true)
                behandlingsresultat {
                    behandletHendelse["type"].asString() shouldBe "Meldekort"
                    førteTil shouldBe Avgjørelse.Endring.toString()
                }
            }

            person.sendInnMeldekort(4)
            meldekortBatch(markerFerdig = true)

            var sisteMeldekortBehandlingId: UUID? = null
            behandlingsresultat {
                sisteMeldekortBehandlingId = behandlingId
                behandletHendelse["type"].asString() shouldBe "Meldekort"
                rettighetsperioder {
                    fra(21.juni(2018)) til stansdato.minusDays(1) harRett true
                    fra(stansdato) harRett false
                }
            }

            val meldingerFørAvsluttetArbeidssøkerperiode = rapidInspektør.size
            val sisteMeldedato = person.fastsattMeldedato(5)
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = sisteMeldedato,
                avsluttetTidspunkt = sisteMeldedato.plusWeeks(3).atTime(12, 21),
            )
            behovsløsere.løsTilForslag()

            val arbeidssøkerperiodeResultat =
                (meldingerFørAvsluttetArbeidssøkerperiode until rapidInspektør.size)
                    .map { rapidInspektør.message(it) }
                    .singleOrNull {
                        it["@event_name"].asString() == "behandlingsresultat" &&
                            it["behandletHendelse"]["type"].asString() == "Arbeidssøkerperiode"
                    } ?: error("Fant ikke behandlingsresultat for Arbeidssøkerperiode")

            var arbeidssøkerperiodeHendelseId: UUID? = null
            with(BehandlingsresultatAssertions(arbeidssøkerperiodeResultat)) {
                behandlingId shouldNotBe sisteMeldekortBehandlingId
                basertPå shouldBe sisteMeldekortBehandlingId
                arbeidssøkerperiodeHendelseId = behandletHendelse["id"].asUUID()
                behandletHendelse["type"].asString() shouldBe "Arbeidssøkerperiode"
                rettighetsperioder {
                    fra(21.juni(2018)) til stansdato.minusDays(1) harRett true
                    fra(stansdato) harRett false
                }
            }
            saksbehandler
                .åpneAvklaringer(arbeidssøkerperiodeHendelseId)
                .shouldBeEmpty()
        }
    }

    @Test
    fun `stanses mens søknaden fortsatt er under behandling`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            // Sier nei i meldekort
            val fastsattMeldedato = person.fastsattMeldedato(2)
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(4).atTime(12, 21),
            )

            // Lukker avklaringen som tvinger alle stans til manuell behandling
            saksbehandler.lukkAlleAvklaringer(person.sisteSøknadId)
            saksbehandler.godkjenn(person.sisteSøknadId)
            saksbehandler.beslutt(person.sisteSøknadId)

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(21.juni(2018)) til 6.juli(2018) harRett true
                }

                opplysning(RegistrertArbeidssøker.registrertArbeidssøker) {
                    fra(21.juni(2018)) til 6.juli(2018) verdi true
                    fra(7.juli(2018)) verdi false
                }
            }
        }
    }

    @Test
    fun `får stans på grunn av ikke meldt uten å ha sendt noen meldekort`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juli(2026), ønskerFraDato = 19.juli(2026))
            behovsløsere.løsTilForslag()

            saksbehandler.leggTilOpplysning(kravPåLønn, true, "", Gyldighetsperiode(21.juli(2026), 31.juli(2026)))
            behovsløsere.løsTilForslag()
            saksbehandler.leggTilOpplysning(kravPåLønn, false, "", Gyldighetsperiode(1.august(2026)))
            behovsløsere.løsTilForslag()
            saksbehandler.leggTilOpplysning(prøvingsdato, 3.august(2026), "", Gyldighetsperiode(3.august(2026)))
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat(1) {
                rettighetsperioder {
                    fra(3.august(2026)) harRett true
                }
            }

            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = 3.august(2026),
                avsluttetTidspunkt = 25.august(2026).atTime(2, 1),
                fristBrutt = true,
            )

            behandlingsresultatForslag(6) {
                rettighetsperioder {
                    fra(3.august(2026)) harRett false
                }
            }

            saksbehandler.leggTilOpplysning(Meldeplikt.oppfyllerMeldeplikt, true, "", Gyldighetsperiode(3.august(2026), 16.august(2026)))
            saksbehandler.leggTilOpplysning(Meldeplikt.oppfyllerMeldeplikt, false, "", Gyldighetsperiode(17.august(2026)))

            behandlingsresultatForslag(8) {
                rettighetsperioder {
                    fra(3.august(2026)) til 16.august(2026) harRett true
                    fra(17.august(2026)) harRett false
                }
            }
        }
    }
}
