package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import org.junit.jupiter.api.Test

class RedigeringTest {
    @Test
    fun `vi kan endre grunnlag og få ny sats`() {
        SimulertDagpengerSystem.Companion
            .nyttScenario {
                inntektSiste12Mnd = 500000
            }.test {
                person.søkDagpenger(21.juni(2018))

                behovsløsere.løsTilForslag()
                saksbehandler.lukkAlleAvklaringer()
                saksbehandler.godkjenn()
                saksbehandler.beslutt()

                vedtak { utfall shouldBe true }
                behandlingsresultat {
                    with(opplysninger(dagsatsEtterSamordningMedBarnetillegg)) {
                        this[0].verdi.verdi shouldBe 1259
                    }
                }

                saksbehandler.lagBehandling(1.juli(2018))
                saksbehandler.endreOpplysning(
                    grunnlag,
                    verdi = Beløp(300000),
                    begrunnelse = "Høres bedre ut",
                    gyldighetsperiode = Gyldighetsperiode(1.juli(2018)),
                )

                behandlingsresultatForslag {
                    with(opplysninger(dagsatsEtterSamordningMedBarnetillegg)) {
                        this[0].verdi.verdi shouldBe 1259

                        this shouldHaveSize 2
                        this[1].verdi.verdi shouldNotBe 1259
                        this[1].verdi.verdi shouldBe 737
                    }
                }

                saksbehandler.endreOpplysning(
                    grunnlag,
                    verdi = Beløp(400000),
                    begrunnelse = "Høres bedre ut",
                    gyldighetsperiode = Gyldighetsperiode(1.juli(2018)),
                )

                behandlingsresultatForslag {
                    with(opplysninger(dagsatsEtterSamordningMedBarnetillegg)) {
                        this[0].verdi.verdi shouldBe 1259

                        this shouldHaveSize 2
                        this[1].verdi.verdi shouldNotBe 1259
                        this[1].verdi.verdi shouldBe 977
                    }
                }
            }
    }
}
