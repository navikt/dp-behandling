package no.nav.dagpenger.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
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
                rettighetsperioder.single().harRett shouldBe true
                rettighetsperioder.single().fraOgMed shouldBe 23.juni(2025)
                rettighetsperioder.single().tilOgMed shouldBe null
            }

            person.sendInnMeldekort(1)
            meldekortBatch(false)

            behandlingsresultatForslag {
                rettighetsperioder.first().harRett shouldBe true
                rettighetsperioder.first().fraOgMed shouldBe 23.juni(2025)
                rettighetsperioder.first().tilOgMed shouldBe 30.juni(2025)

                rettighetsperioder.last().harRett shouldBe false
                rettighetsperioder.last().fraOgMed shouldBe 1.juli(2025)
                rettighetsperioder.last().tilOgMed shouldBe null

                førteTil shouldBe "Stans"
                with(opplysninger(Alderskrav.kravTilAlder)) {
                    this.size shouldBe 2
                    this.first().verdi.verdi shouldBe true
                    this.first().gyldigFraOgMed shouldBe 23.juni(2025)
                    this.first().gyldigTilOgMed shouldBe 30.juni(2025)

                    this.last().verdi.verdi shouldBe false
                    this.last().gyldigFraOgMed shouldBe 1.juli(2025)
                    this.last().gyldigTilOgMed shouldBe null
                }

                val sisteUtbetalteDag =
                    utbetalinger
                        .filter { it.path("utbetaling").asInt() > 0 }
                        .maxOfOrNull { it.path("dato").asLocalDate() }
                sisteUtbetalteDag shouldBe 30.juni(2025)
            }
            saksbehandler.åpneAvklaringer().map { it.kode } shouldBe listOf("MeldekortBehandling", "StansAlder")
        }
    }
}
