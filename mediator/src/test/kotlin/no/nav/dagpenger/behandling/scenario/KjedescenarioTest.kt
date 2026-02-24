package no.nav.dagpenger.behandling.scenario

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.dagpenger.behandling.august
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.helpers.scenario.assertions.Opplysningsperiode.Periodestatus.Arvet
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien
import org.junit.jupiter.api.Test
import java.util.UUID

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

            behandlingsresultat { utfall shouldBe true }

            val sisteFerdige = person.behandlingId

            var førsteBehandling: UUID? = null
            // Personen søker tre ganger før forrige søknad er behandlet
            repeat(3) {
                person.søkDagpenger(21.august(2018))
                if (førsteBehandling == null) {
                    førsteBehandling = person.behandlingId
                }
                behovsløsere.løsTilForslag()
                saksbehandler.lukkAlleAvklaringer()
                person.behandling.basertPå shouldBe sisteFerdige
            }

            // Ferdigstill en av de parallelle behandlingene
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val exception =
                shouldThrow<IllegalStateException> {
                    val eksternId = UUID.fromString(person.behandling(førsteBehandling!!).behandletHendelse.id)
                    // Kan ikke godkjenne når det finnes flere parallelle behandlinger
                    saksbehandler.godkjenn(eksternId)
                }

            exception.message shouldContain "nyere åpen behandling"
        }
    }

    @Test
    fun `kan fatte parallell når det er avslag`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            // Personen søker tre ganger etter forrige søknad er behandlet
            repeat(3) {
                person.søkDagpenger(21.august(2018))
                behovsløsere.løsTilForslag()
                saksbehandler.lukkAlleAvklaringer()
                person.behandling.basertPå.shouldBeNull()
            }

            // Ferdigstill en av de parallelle behandlingene
            saksbehandler.godkjenn()
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

            behandlingsresultat { utfall shouldBe false }

            // Personen søker tre ganger før forrige søknad er behandlet
            repeat(3) {
                person.søkDagpenger(21.august(2018))
                behovsløsere.løsTilForslag()
                person.behandling.basertPå.shouldBeNull()
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

            behandlingsresultat { utfall shouldBe true }

            val sisteFerdige = person.behandlingId

            // Søknad behandles som gjenopptak på forrige behandling
            person.søkDagpenger(21.august(2020))
            behovsløsere.løsTilForslag()
            person.behandling.basertPå shouldBe sisteFerdige

            // Søknad kan ikke gjenoppta fordi forrige periode er for lenge siden
            // Saksbehandler må manuelt flytte behandlingen til en ny kjede
            saksbehandler.flyttBehandlingTilNyKjede(behandlingId = person.behandlingId, fagsakId = 123)

            // Ny kjede fører til manglende opplysninger som må innhentes på nytt
            behovsløsere.løsTilForslag()
            person.behandling.basertPå.shouldBeNull()
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

            behandlingsresultat { utfall shouldBe true }
            val behandling1 = person.behandlingId

            // Behandling 2
            person.søkDagpenger(21.juni(2015))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat { utfall shouldBe true }
            val behandling2 = person.behandlingId

            // Behandling 3 blir nå kjedet på behandling 2, men skal flyttes til behandling 1
            person.søkDagpenger(21.august(2020))
            behovsløsere.løsTilForslag()
            person.behandling.basertPå shouldBe behandling2

            // Saksbehandler flytter behandling 3 til behandling 1, og hopper over behandling 2
            saksbehandler.flyttBehandlingTilNyKjede(
                behandlingId = person.behandlingId,
                nyBehandlingskjedeId = behandling1,
            )

            // Ny kjede fører til manglende opplysninger som må innhentes på nytt
            // behovsløsere.løsTilForslag()
            person.behandling.basertPå shouldBe behandling1
        }
    }

    @Test
    fun `søknad om dagpenger uten ferdig behandling gir ny kjede`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2015))
            behovsløsere.løsTilForslag()

            // Søker på nytt før forrige behandling er ferdig
            person.søkDagpenger(21.juni(2015))
            behovsløsere.løsTilForslag()

            // Ny behandling har ikke blitt kjedet på forrige behandling
            person.behandling.basertPå.shouldBeNull()
        }
    }

    @Test
    fun `søknad om dagpenger etter avslag gir ny kjede`() {
        nyttScenario {
            inntektSiste12Mnd = 50000
        }.test {
            person.søkDagpenger(21.juni(2015))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            // Søker på nytt etter avslag, får ny kjede
            person.søkDagpenger(21.juni(2015))
            behovsløsere.løsTilForslag()

            // Ny behandling har ikke blitt kjedet på forrige behandling
            person.behandling.basertPå.shouldBeNull()
        }
    }

    @Test
    fun `søknad om dagpenger etter innvilgelse bygger videre på eksisterende kjede`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(11.juni(2015))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder.single().harRett shouldBe true

                with(opplysninger(Minsteinntekt.minsteinntekt)) {
                    this shouldHaveSize 1
                    single().gyldigFraOgMed shouldBe 11.juni(2015)
                }

                opplysninger(PermitteringFraFiskeindustrien.godkjentÅrsakPermitteringFraFiskindustri) shouldHaveSize 0
            }

            val behandling1 = person.behandlingId

            // Søker på nytt etter innvilgelse, blir kjedet på forrige behandling
            person.søkGjenopptak(21.juni(2015))

            // Opplysninger fra søknaden hentes inn på nytt
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                // Ny behandling har blitt kjedet på forrige behandling
                basertPå shouldBe behandling1

                // Vi gjenbruker vurderingen av minsteinntekt fra forrige behandling
                with(opplysninger(Minsteinntekt.minsteinntekt)) {
                    this shouldHaveSize 1
                    single().gyldigFraOgMed shouldBe 11.juni(2015)
                    single().verdi.verdi shouldBe true
                    single().opprinnelse shouldBe Arvet
                }

                with(opplysninger(Opphold.oppfyllerKravet)) {
                    this shouldHaveSize 2
                }

                opplysninger(PermitteringFraFiskeindustrien.godkjentÅrsakPermitteringFraFiskindustri).shouldBeEmpty()
            }
        }
    }

    @Test
    fun `søker gjenopptak uten rettighetsperiode`() {
        nyttScenario { }.test {
            person.søkGjenopptak(21.juni(2018))

            // Disse skal bare avvises i døra
            rapidInspektør.size shouldBe 0
        }
    }

    @Test
    fun `søker gjenopptak med eksisterende rettighetsperiode`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val behandlingIdInnvilgelse = person.behandlingId
            person.søkGjenopptak(22.juni(2018))
            behovsløsere.løsTilForslag()

            behandlingsresultatForslag {
                basertPå shouldBe behandlingIdInnvilgelse
            }
        }
    }
}
