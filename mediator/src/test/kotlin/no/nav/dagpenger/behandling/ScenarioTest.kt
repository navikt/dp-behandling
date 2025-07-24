package no.nav.dagpenger.behandling

import io.kotest.matchers.collections.shouldNotBeEmpty
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
    fun `flere søknader på eksisterende kjede skal opprettes i parallell`() {
        /*
         * Når bruker har en behandlingskjede med invilget vedtak, og søker på nytt
         * skal det opprettes en ny behandling for hver søknad som kjedes på siste ferdige.
         *
         * Bare en (parallell) behandling kan bli ferdigstilt.
         * De andre parallelle behandlinge må avbrytes.
         * Saksbehandler må manuelt avbryte behandlingene som ikke skal behandles.
         */
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

            // Personen søker tre ganger før forrige søknad er behandlet
            repeat(3) {
                person.søkDagpenger(21.august(2018))
                behovsløsere.løsTilForslag()
                person.behandling.basertPåBehandlinger.shouldNotBeEmpty()
            }
        }
    }

    @Test
    fun `bruker kan ikke gjenoppta fordi forrige periode er for lenge siden`() {
        /*
         * Når bruker søker og kan ikke gjenoppta fordi forrige periode er for lenge siden.
         *
         * Behandlingen blir kjedet på forrige ferdige behandling.
         * Saksbehandler må manuelt flytte behandlingen til en ny kjede.
         */
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2015))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            vedtak {
                utfall shouldBe true
            }

            person.søkDagpenger(21.august(2020))

            // TODO: Fiks dette
            // saksbehandler.flyttBehandlingTilNyKjede()
        }
    }
}
