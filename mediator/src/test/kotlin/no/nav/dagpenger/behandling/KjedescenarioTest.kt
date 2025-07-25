package no.nav.dagpenger.behandling

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Test

// Tester ulike scenarier for kjeding av behandlinger
class KjedescenarioTest {
    @Test
    fun `flere søknader på eksisterende kjede skal opprettes i parallell`() {
        /*
         * Når bruker har en behandlingskjede med vedtak om innvilgelse, og søker på nytt
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

            val sisteFerdige = person.behandlingId

            // Personen søker tre ganger før forrige søknad er behandlet
            repeat(3) {
                person.søkDagpenger(21.august(2018))
                behovsløsere.løsTilForslag()
                person.behandling.basertPåBehandlinger.shouldContainExactly(sisteFerdige)
                person.behandling.basertPåBehandling shouldBe sisteFerdige
            }
        }
    }

    @Test
    fun `flere søknader skal ikke kjedes når siste behandling var avslag`() {
        /*
         * Når bruker har en behandlingskjede med vedtak om avslag, og søker på nytt
         * skal det opprettes en ny behandling for hver søknad som ikke kjedes på siste ferdige.
         *
         * Saksbehandler må manuelt avbryte behandlingene som ikke skal behandles.
         */
        nyttScenario {
            inntektSiste12Mnd = 5000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            vedtak {
                utfall shouldBe false
            }

            // Personen søker tre ganger før forrige søknad er behandlet
            repeat(3) {
                person.søkDagpenger(21.august(2018))
                behovsløsere.løsTilForslag()
                person.behandling.basertPåBehandlinger.shouldBeEmpty()
                person.behandling.basertPåBehandling.shouldBeNull()
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

            val sisteFerdige = person.behandlingId

            // Søknad behandles som gjenopptak på forrige behandling
            person.søkDagpenger(21.august(2020))
            behovsløsere.løsTilForslag()
            person.behandling.basertPåBehandlinger.shouldContainExactly(sisteFerdige)
            person.behandling.basertPåBehandling shouldBe sisteFerdige

            // Søknad kan ikke gjenoppta fordi forrige periode er for lenge siden
            // Saksbehandler må manuelt flytte behandlingen til en ny kjede
            saksbehandler.flyttBehandlingTilNyKjede(behandlingId = person.behandlingId, fagsakId = 123)

            // Ny kjede fører til manglende opplysninger som må innhentes på nytt
            behovsløsere.løsTilForslag()
            person.behandling.basertPåBehandling.shouldBeNull()
        }
    }

    @Test
    fun `behandling kan flyttes fra ny til eksisterende kjede`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            // Behandling 1
            person.søkDagpenger(20.juni(2014))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            vedtak {
                utfall shouldBe true
            }
            val behandling1 = person.behandlingId

            // Behandling 2
            person.søkDagpenger(21.juni(2015))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            vedtak {
                utfall shouldBe true
            }
            val behandling2 = person.behandlingId

            // Behandling 3 blir nå kjedet på behandling 2, men skal flyttes til behandling 1
            person.søkDagpenger(21.august(2020))
            behovsløsere.løsTilForslag()
            person.behandling.basertPåBehandling shouldBe behandling2

            // Saksbehandler flytter behandling 3 til behandling 1, og hopper over behandling 2
            saksbehandler.flyttBehandlingTilNyKjede(behandlingId = person.behandlingId, nyBehandlingskjedeId = behandling1)

            // Ny kjede fører til manglende opplysninger som må innhentes på nytt
            // behovsløsere.løsTilForslag()
            person.behandling.basertPåBehandling shouldBe behandling1
        }
    }
}
