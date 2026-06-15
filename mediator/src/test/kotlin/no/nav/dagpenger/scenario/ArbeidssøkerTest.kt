package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.api.models.OpprinnelseDTO
import no.nav.dagpenger.mediator.api.models.RettighetsperiodeDTO
import no.nav.dagpenger.mediator.asUUID
import no.nav.dagpenger.mediator.august
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse.Companion.behandlingFlyttetAvklaring
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.Meldeplikt
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Disabled
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
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().tilOgMed shouldBe null
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

            behandlingsresultatForslag {
                with(opplysninger(Meldeplikt.oppfyllerMeldeplikt)) {
                    this shouldHaveSize 2
                }
                with(opplysninger(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker)) {
                    this shouldHaveSize 2
                    this[1].verdi.verdi shouldBe false
                    this[1].gyldigFraOgMed shouldBe 19.juli(2018)
                }
                // Stansen skal være fra og med torsdag
                rettighetsperioder.shouldContainExactly(
                    RettighetsperiodeDTO(
                        fraOgMed = 21.juni(2018),
                        tilOgMed = 18.juli(2018),
                        harRett = true,
                        opprinnelse = OpprinnelseDTO.ARVET,
                    ),
                    RettighetsperiodeDTO(
                        fraOgMed = 19.juli(2018),
                        tilOgMed = null,
                        harRett = false,
                        opprinnelse = OpprinnelseDTO.NY,
                    ),
                )
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
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().tilOgMed shouldBe null
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
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                with(opplysninger(Meldeplikt.oppfyllerMeldeplikt)) {
                    this shouldHaveSize 2
                    this[0].verdi.verdi shouldBe true
                    this[1].verdi.verdi shouldBe false
                    this[1].gyldigFraOgMed shouldBe 16.juli(2018)
                }
                with(opplysninger(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker)) {
                    this shouldHaveSize 2
                    this[1].verdi.verdi shouldBe false
                    this[1].gyldigFraOgMed shouldBe 1.august(2018)
                }

                rettighetsperioder shouldHaveSize 3
                rettighetsperioder[1].harRett shouldBe true
                rettighetsperioder[1].tilOgMed shouldBe 15.juli(2018)
                rettighetsperioder[2].harRett shouldBe false
                rettighetsperioder[2].fraOgMed shouldBe 16.juli(2018)
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
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().tilOgMed shouldBe null
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
                    this shouldHaveSize 2
                }
            }

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Melder seg for periode 3 i tide, men nei som videre registrert
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(16).atTime(12, 21),
            )

            // Lukker avklaringen som tvinger alle stans til manuell behandling
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat(nummer = 4) {
                with(opplysninger(Meldeplikt.oppfyllerMeldeplikt)) {
                    this shouldHaveSize 2
                }
                with(opplysninger(RegistrertArbeidssøker.registrertArbeidssøker)) {
                    this shouldHaveSize 2
                }
                with(opplysninger(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker)) {
                    this shouldHaveSize 2
                    this[1].verdi.verdi shouldBe false
                    this[1].gyldigFraOgMed shouldBe 1.august(2018)
                }

                rettighetsperioder shouldHaveSize 2

                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].tilOgMed shouldBe 15.juli(2018)
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 16.juli(2018)
            }
        }
    }

    @Test
    @Disabled("Denne gir ikke verdi før meldekort kan gå automatisk")
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
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().tilOgMed shouldBe null
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

            // Simulerer at vi tar imot flytt_behandling og flytter søsken
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
                rettighetsperioder shouldHaveSize 1
                with(rettighetsperioder.single()) {
                    harRett shouldBe true
                    fraOgMed shouldBe 21.juni(2018)
                    tilOgMed shouldBe 5.juli(2018)
                }

                with(opplysninger(RegistrertArbeidssøker.registrertArbeidssøker)) {
                    this shouldHaveSize 2

                    this[0].gyldigFraOgMed shouldBe 21.juni(2018)
                    this[1].gyldigFraOgMed shouldBe 6.juli(2018)
                }
            }
        }
    }
}
