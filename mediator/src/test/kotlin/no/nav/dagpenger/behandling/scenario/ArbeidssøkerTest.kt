package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.api.models.OpprinnelseDTO
import no.nav.dagpenger.behandling.api.models.RettighetsperiodeDTO
import no.nav.dagpenger.behandling.august
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.regel.Meldeplikt
import no.nav.dagpenger.regel.RegistrertArbeidssøker
import org.junit.jupiter.api.Test

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
            meldekortBatch(true)
            person.sendInnMeldekort(2)
            meldekortBatch(true)

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Blir avregistrert av veileder første torsdag i meldeperiode 3
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(3).atTime(12, 21),
                manueltAvregistrert = true,
            )

            behandlingsresultatForslag {
                with(opplysninger(Meldeplikt.oppfyllerMeldeplikt)) {
                    this shouldHaveSize 0
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
            meldekortBatch(true)
            person.sendInnMeldekort(2)
            meldekortBatch(true)

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
                    this shouldHaveSize 1
                    this.single().verdi.verdi shouldBe false
                    this.single().gyldigFraOgMed shouldBe 16.juli(2018)
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

            behandlingsresultat {
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().tilOgMed shouldBe null
            }

            person.sendInnMeldekort(1)
            meldekortBatch(true)
            person.sendInnMeldekort(2)
            meldekortBatch(true)

            val fastsattMeldedato = person.fastsattMeldedato(3)

            // Melder seg for periode 3 i tide, men nei som videre registrert
            person.avsluttArbeidssøkerperiode(
                fastsattMeldingsdag = fastsattMeldedato,
                avsluttetTidspunkt = fastsattMeldedato.plusDays(16).atTime(12, 21),
            )

            // Lukker avklaringen som tvinger alle stans til manuell behandling
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                with(opplysninger(Meldeplikt.oppfyllerMeldeplikt)) {
                    this shouldHaveSize 0
                }
                with(opplysninger(RegistrertArbeidssøker.oppyllerKravTilRegistrertArbeidssøker)) {
                    this shouldHaveSize 2
                    this[1].verdi.verdi shouldBe false
                    this[1].gyldigFraOgMed shouldBe 1.august(2018)
                }

                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].tilOgMed shouldBe 31.juli(2018)
                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 1.august(2018)
            }
        }
    }
}
