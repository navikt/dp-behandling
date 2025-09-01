package no.nav.dagpenger.behandling

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.scenario.assertions.Opplysningsperiode.Periodestatus
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.regel.Alderskrav.fødselsdato
import no.nav.dagpenger.regel.Behov
import no.nav.dagpenger.regel.Opphold
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
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            vedtak {
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
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            vedtak {
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
    fun `tester innvilgelse etter endring `() {
        nyttScenario {
            inntektSiste12Mnd = 500000
            godkjennMeldinger = false
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.endreOpplysning(
                opplysningstype = Opphold.oppholdINorge,
                verdi = false,
                gyldighetsperiode = Gyldighetsperiode(tom = 24.juni(2018)),
            )
            saksbehandler.endreOpplysning(
                opplysningstype = Opphold.oppholdINorge,
                verdi = true,
                gyldighetsperiode = Gyldighetsperiode(fom = 25.juni(2018)),
            )
            klumpen {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe false
                rettighetsperioder[0].tilOgMed shouldBe 24.juni(2018)
                rettighetsperioder[1].harRett shouldBe true
                rettighetsperioder[1].fraOgMed shouldBe 25.juni(2018)
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

            klumpen {
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                opplysninger(Opphold.oppholdINorge) shouldHaveSize 1
            }

            // Opprett stans
            person.opprettBehandling(22.juli(2018))
            behovsløsere.løsningFor(Behov.OppholdINorge, false, 22.juli(2018))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            klumpen {
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 22.juli(2018)

                with(opplysninger(Opphold.oppholdINorge)) {
                    this shouldHaveSize 2
                    this[0].status shouldBe Periodestatus.Arvet
                    this[1].status shouldBe Periodestatus.Ny
                }
                with(opplysninger(Opphold.oppfyllerKravetTilOpphold)) {
                    this[0].verdi.verdi shouldBe "true"
                    this[1].verdi.verdi shouldBe "false"
                }
            }

            // Gjenoppta
            person.opprettBehandling(23.august(2018))
            behovsløsere.løsningFor(Behov.OppholdINorge, true, 23.august(2018))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            klumpen {
                with(opplysninger(Opphold.oppfyllerKravetTilOpphold)) {
                    this[0].verdi.verdi shouldBe "true"
                    this[1].verdi.verdi shouldBe "false"
                    this[2].verdi.verdi shouldBe "true"
                }

                rettighetsperioder shouldHaveSize 3
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 22.juli(2018)

                rettighetsperioder[2].harRett shouldBe true
                rettighetsperioder[2].fraOgMed shouldBe 23.august(2018)

                with(opplysninger(Opphold.oppholdINorge)) {
                    this shouldHaveSize 3
                    this[0].status shouldBe Periodestatus.Arvet
                    this[1].status shouldBe Periodestatus.Arvet
                    this[2].status shouldBe Periodestatus.Ny
                }
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
}
