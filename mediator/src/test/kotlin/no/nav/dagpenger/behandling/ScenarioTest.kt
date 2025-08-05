package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.regel.Alderskrav.fødselsdato
import no.nav.dagpenger.regel.Behov
import org.junit.jupiter.api.Test

class ScenarioTest {
    @Test
    fun `tester avslag ved for høy alder`() {
        nyttScenario {
            alder = 88
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            forslag {
                utfall shouldBe false
            }
        }
    }

    @Test
    fun `tester avslag ved for lite inntekt`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            forslag {
                utfall shouldBe false
            }
        }
    }

    @Test
    fun `tester innvilgelse`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            vedtak {
                utfall shouldBe true
            }
        }
    }

    @Test
    fun `tester innvilgelse ved permittering`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            permittering = true
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            forslag {
                utfall shouldBe true

                medFastsettelser {
                    periode("Permitteringsperiode") shouldBe 26
                }
            }
        }
    }

    @Test
    fun `tester innvilgelse, stans, og gjenopptak `() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            vedtak {
                utfall shouldBe true
            }

            // Opprett stans
            person.opprettBehandling(22.juli(2018))
            behovsløsere.løsningFor(Behov.RegistrertSomArbeidssøker, false, 22.juli(2018))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            vedtak {
                utfall shouldBe false
            }

            // Gjenoppta
            person.opprettBehandling(23.august(2018))
            behovsløsere.løsningFor(Behov.RegistrertSomArbeidssøker, true, 23.august(2018))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            vedtak {
                utfall shouldBe true
            }
        }
    }

    @Test
    fun `Fjerning og redigering av opplysninger`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            val opplysning = saksbehandler.fjernOpplysning(fødselsdato)
            person.behandling.harOpplysning(opplysning.id) shouldBe false
        }
    }

    @Test
    fun `Endring av prøvingsdato i kjedet behandling`() {
        /**
         * Test som fanger opp at endringer i kjedet behandling ender opp med flere opplysninger av samme type
         */
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            var originalVurdering: List<JsonNode>? = null
            vedtak {
                utfall shouldBe true
                originalVurdering = opplysning("Oppfyller kravet til alder")
            }

            // Lag ny behadnling og endre prøvingsdato
            person.opprettBehandling(21.juni(2018))
            behovsløsere.løsningFor(Behov.Prøvingsdato, 25.juni(2018), 25.juni(2018))
            behovsløsere.løsningFor(Behov.Prøvingsdato, 21.juni(2018), 21.juni(2018))

            forslag {
                with(opplysning("Oppfyller kravet til alder")) {
                    this shouldHaveSize 1
                    this.first()["verdi"] shouldBe originalVurdering!!.first()["verdi"]
                    this.first()["gyldigFraOgMed"] shouldBe originalVurdering!!.first()["gyldigFraOgMed"]
                }
            }
        }
    }
}
