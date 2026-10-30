package no.nav.dagpenger.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juli
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import kotlin.test.Test

class StansPgaAlderTest {
    @Test
    fun `meldekort samme måned som blir 67`() {
        nyttScenario {
            fødselsdato = 1.juni(1958)
            inntektSiste12Mnd = 5000000
        }.test {
            person.søkDagpenger(dato = 23.juni(2025))

            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            behandlingsresultat {
                rettighetsperioder {
                    fra(23.juni(2025)) harRett true
                }
            }

            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = false)

            behandlingsresultatForslag {
                rettighetsperioder {
                    fra(23.juni(2025)) til 30.juni(2025) harRett true
                    fra(1.juli(2025)) harRett false
                }

                førteTil shouldBe "Stans"
                opplysning(Alderskrav.kravTilAlder) {
                    fra(23.juni(2025)) til 30.juni(2025) verdi true
                    fra(1.juli(2025)) verdi false
                }

                val sisteUtbetalteDag =
                    utbetalinger
                        .filter { it.path("utbetaling").asInt() > 0 }
                        .maxOfOrNull { it.path("dato").asLocalDate() }
                sisteUtbetalteDag shouldBe 30.juni(2025)
            }
            saksbehandler.åpneAvklaringer().map { it.kode } shouldContain "StansAlder"
        }
    }
}
