package no.nav.dagpenger.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juni
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
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().fraOgMed shouldBe 23.juni(2025)
                rettighetsperioder.single().tilOgMed shouldBe 30.juni(2025)
            }

            person.sendInnMeldekort(1)
            meldekortBatch(true)

            behandlingsresultatForslag {
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().fraOgMed shouldBe 23.juni(2025)
                rettighetsperioder.single().tilOgMed shouldBe 30.juni(2025)

                val sisteUtbetalteDag =
                    utbetalinger
                        .filter { it.path("utbetaling").asInt() > 0 }
                        .maxOfOrNull { it.path("dato").asLocalDate() }
                sisteUtbetalteDag shouldBe 30.juni(2025)
            }
        }
    }
}
