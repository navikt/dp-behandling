package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.august
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.helpers.scenario.assertions.Opplysningsperiode.Periodestatus
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.behandling.scenario.ScenarioTest.Formatter.lagBrev
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Inntekt
import no.nav.dagpenger.regel.Gjenopptak
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Minsteinntekt.inntektFraSkatt
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.Opphold.oppholdINorge
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import org.junit.jupiter.api.Test

class GjenopptakTest {
    @Test
    fun `tester innvilgelse, stans, og gjenopptak`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val innvilgelseBehandlingId = person.behandlingId
            behandlingsresultat {
                førteTil shouldBe "Innvilgelse"
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                opplysninger(oppholdINorge) shouldHaveSize 1
            }

            // Må ha startet forbruk for å kunne gjenoppta
            person.sendInnMeldekort(1)
            meldekortBatch(true)

            // Opprett stans
            person.opprettBehandling(22.juli(2018))
            saksbehandler.endreOpplysning(oppholdINorge, false, "Er i utlandet", Gyldighetsperiode(22.juli(2018)))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                førteTil shouldBe "Stans"
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 22.juli(2018)

                with(opplysninger(oppholdINorge)) {
                    this shouldHaveSize 2
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[1].opprinnelse shouldBe Periodestatus.Ny
                }
                with(opplysninger(Opphold.oppfyllerKravetTilOpphold)) {
                    this[0].verdi.verdi shouldBe true
                    this[1].verdi.verdi shouldBe false
                }
            }

            // Gjenoppta
            person.søkGjenopptak(23.august(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.endreOpplysning(oppholdINorge, true, "Tilbake fra utlandet", Gyldighetsperiode(23.august(2018)))
            saksbehandler.endreOpplysning(harLøpendeRett, true, "Har krav", Gyldighetsperiode(23.august(2018)))
            /*saksbehandler.endreOpplysning(
                oppholdMedArbeidI12ukerEllerMer,
                true,
                "Skal inntekte inntekt på nytt",
                Gyldighetsperiode(23.august(2018)),
            )*/

            // behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                behandlingskjedeId shouldBe innvilgelseBehandlingId
                førteTil shouldBe "Gjenopptak"

                with(opplysninger(Gjenopptak.skalGjenopptas)) {
                    this shouldHaveSize 1
                    this.single().verdi.verdi shouldBe true
                    this.single().opprinnelse shouldBe Periodestatus.Ny
                }

                with(opplysninger(inntektFraSkatt)) { this shouldHaveSize 2 }
                with(opplysninger(Minsteinntekt.minsteinntekt)) { this shouldHaveSize 1 }
                with(opplysninger(grunnlag)) {
                    this shouldHaveSize 2
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[0].verdi.verdi shouldBe 517349

                    // Den nye inntekten er ikke nok å bli valgt
                    this[1].opprinnelse shouldBe Periodestatus.Ny
                    this[1].verdi.verdi shouldBe this[0].verdi.verdi
                }

                with(opplysninger(Opphold.oppfyllerKravetTilOpphold)) {
                    this[0].verdi.verdi shouldBe true
                    this[1].verdi.verdi shouldBe false
                    this[2].verdi.verdi shouldBe true
                }

                rettighetsperioder shouldHaveSize 3
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 22.juli(2018)

                rettighetsperioder[2].harRett shouldBe true
                rettighetsperioder[2].fraOgMed shouldBe 23.august(2018)

                with(opplysninger(oppholdINorge)) {
                    this shouldHaveSize 3
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[1].opprinnelse shouldBe Periodestatus.Arvet
                    this[2].opprinnelse shouldBe Periodestatus.Ny
                }
            }
        }
    }

    @Test
    fun `tester å hente inn inntekt på nytt ved gjenopptak`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Opprett stans
            person.opprettBehandling(22.juli(2018))
            saksbehandler.endreOpplysning(oppholdINorge, false, "Er i utlandet", Gyldighetsperiode(22.juli(2018)))
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Gjenoppta
            val gjenopptaksdato = 23.august(2018)
            person.søkGjenopptak(gjenopptaksdato)
            behovsløsere.løsTilForslag()
            saksbehandler.endreOpplysning(oppholdINorge, true, "Tilbake fra utlandet", Gyldighetsperiode(gjenopptaksdato))
            saksbehandler.endreOpplysning(harLøpendeRett, true, "Har krav", Gyldighetsperiode(gjenopptaksdato))

            // Legg inn en inntekt som gir et lavere grunnlag
            val lavereInntekt = person.inntekt(300000, gjenopptaksdato.minusMonths(2))
            saksbehandler.endreOpplysning(inntektFraSkatt, Inntekt(lavereInntekt), "Har krav", Gyldighetsperiode(gjenopptaksdato))

            behandlingsresultatForslag {
                with(opplysninger(Minsteinntekt.minsteinntekt)) { this shouldHaveSize 1 }
                with(opplysninger(grunnlag)) {
                    this shouldHaveSize 2
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[0].verdi.verdi shouldBe 517349

                    // Den nye inntekten er ikke nok å bli valgt
                    this[1].opprinnelse shouldBe Periodestatus.Ny
                    this[1].verdi.verdi shouldBe this[0].verdi.verdi
                }

                with(opplysninger(dagsatsEtterSamordningMedBarnetillegg)) {
                    this shouldHaveSize 2
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[1].opprinnelse shouldBe Periodestatus.Ny

                    // Sats endres heller ikke
                    this[1].verdi.verdi shouldBe this[0].verdi.verdi
                }
            }

            // Legg inn en inntekt som gir et høyere grunnlag
            val høyereInntekt = person.inntekt(600000, gjenopptaksdato.minusMonths(2))
            saksbehandler.endreOpplysning(inntektFraSkatt, Inntekt(høyereInntekt), "Har krav", Gyldighetsperiode(gjenopptaksdato))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                førteTil shouldBe "Gjenopptak"

                with(opplysninger(Minsteinntekt.minsteinntekt)) { this shouldHaveSize 1 }
                with(opplysninger(dagpengegrunnlag)) {
                    this shouldHaveSize 2
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[1].opprinnelse shouldBe Periodestatus.Ny
                }

                with(opplysninger(grunnlag)) {
                    this shouldHaveSize 2
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[0].verdi.verdi shouldBe 517349

                    this[1].opprinnelse shouldBe Periodestatus.Ny
                    this[1].verdi.verdi shouldBe 581298
                }

                with(opplysninger(dagsatsEtterSamordningMedBarnetillegg)) {
                    this shouldHaveSize 2
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[1].opprinnelse shouldBe Periodestatus.Ny

                    (this[1].verdi.verdi as Int) shouldBeGreaterThan (this[0].verdi.verdi as Int)
                }
            }
        }
    }

    @Test
    fun `tester innvilgelse, stans, og avslag på gjenopptak skal føre til avslag`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))

            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            val innvilgelseBehandlingId = person.behandlingId
            behandlingsresultat {
                førteTil shouldBe "Innvilgelse"
                rettighetsperioder shouldHaveSize 1
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                opplysninger(oppholdINorge) shouldHaveSize 1

                lagBrev(klump.toString()).also { println(it) }
            }

            // Opprett stans
            person.opprettBehandling(22.juli(2018))
            saksbehandler.endreOpplysning(oppholdINorge, false, "Er i utlandet", Gyldighetsperiode(22.juli(2018)))

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                førteTil shouldBe "Stans"
                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 22.juli(2018)

                lagBrev(klump.toString()).also {
                    println(it)
                }

                with(opplysninger(oppholdINorge)) {
                    this shouldHaveSize 2
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[1].opprinnelse shouldBe Periodestatus.Ny
                }
                with(opplysninger(Opphold.oppfyllerKravetTilOpphold)) {
                    this[0].verdi.verdi shouldBe true
                    this[1].verdi.verdi shouldBe false
                }
            }

            // Gjenoppta
            person.søkGjenopptak(23.august(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.endreOpplysning(
                oppholdINorge,
                false,
                "Sa han var tilbake fra utlandet, men det var han ikke",
                Gyldighetsperiode(23.august(2018)),
            )

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                behandlingskjedeId shouldBe innvilgelseBehandlingId
                førteTil shouldBe "Avslag"

                with(opplysninger(Opphold.oppfyllerKravetTilOpphold)) {
                    this[0].verdi.verdi shouldBe true
                    this[1].verdi.verdi shouldBe false
                    this[2].verdi.verdi shouldBe false
                }

                lagBrev(klump.toString()).also { println(it) }

                rettighetsperioder shouldHaveSize 2
                rettighetsperioder[0].harRett shouldBe true
                rettighetsperioder[0].fraOgMed shouldBe 21.juni(2018)

                rettighetsperioder[1].harRett shouldBe false
                rettighetsperioder[1].fraOgMed shouldBe 22.juli(2018)

                opplysninger shouldHaveSize 220

                with(opplysninger(oppholdINorge)) {
                    this shouldHaveSize 3
                    this[0].opprinnelse shouldBe Periodestatus.Arvet
                    this[1].opprinnelse shouldBe Periodestatus.Arvet
                    this[2].opprinnelse shouldBe Periodestatus.Ny
                }
            }
        }
    }
}
