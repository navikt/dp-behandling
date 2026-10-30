package no.nav.dagpenger.scenario

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag.grunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
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

                behandlingsresultat { utfall shouldBe true }
                behandlingsresultat {
                    opplysning(dagsatsEtterSamordningMedBarnetillegg) {
                        fra(21.juni(2018)) verdi Beløp(1242)
                    }
                }

                saksbehandler.lagBehandling(1.juli(2018))
                saksbehandler.leggTilOpplysning(
                    grunnlag,
                    verdi = Beløp(300000),
                    begrunnelse = "Høres bedre ut",
                    gyldighetsperiode = Gyldighetsperiode(1.juli(2018)),
                )

                behandlingsresultatForslag {
                    opplysning(dagsatsEtterSamordningMedBarnetillegg) {
                        fra(21.juni(2018)) til 30.juni(2018) verdi Beløp(1242)
                        fra(1.juli(2018)) verdi Beløp(720)
                    }
                }

                saksbehandler.leggTilOpplysning(
                    grunnlag,
                    verdi = Beløp(400000),
                    begrunnelse = "Høres bedre ut",
                    gyldighetsperiode = Gyldighetsperiode(1.juli(2018)),
                )

                behandlingsresultatForslag {
                    opplysning(dagsatsEtterSamordningMedBarnetillegg) {
                        fra(21.juni(2018)) til 30.juni(2018) verdi Beløp(1242)
                        fra(1.juli(2018)) verdi Beløp(960)
                    }
                }
                saksbehandler.lukkAlleAvklaringer()
                saksbehandler.godkjenn()
                saksbehandler.beslutt()

                saksbehandler.lagBehandling(10.juli(2018))
                saksbehandler.leggTilOpplysning(
                    grunnlag,
                    verdi = Beløp(300000),
                    begrunnelse = "Høres bedre ut",
                    gyldighetsperiode = Gyldighetsperiode(10.juli(2018)),
                )

                behandlingsresultatForslag {
                    opplysning(dagsatsEtterSamordningMedBarnetillegg) {
                        fra(21.juni(2018)) til 30.juni(2018) verdi Beløp(1242)
                        fra(1.juli(2018)) til 9.juli(2018) verdi Beløp(960)
                        fra(10.juli(2018)) verdi Beløp(720)
                    }
                }
            }
    }
}
