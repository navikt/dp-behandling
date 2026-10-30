package no.nav.dagpenger.scenario

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold.oppfyllerKravet
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MigreringTest {
    @Disabled
    @Test
    fun `tester meldekort etter migrering`() {
        nyttScenario {
            inntektSiste12Mnd = 500000
        }.test {
            person.startMigrering("123123", 21.juni(2018))

            behovsløsere.aktiveBehov() shouldHaveSize 0

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            val innvilgelseBehandlingId = person.behandlingId
            behandlingsresultat(1) {
                førteTil shouldBe "Innvilgelse"
                rettighetsperioder {
                    fra(21.juni(2018)) harRett true
                }

                opplysninger(oppfyllerKravet) shouldHaveSize 1
            }

            // Må ha startet forbruk for å kunne gjenoppta
            person.sendInnMeldekort(1)
            meldekortBatch(markerFerdig = true)

            behandlingsresultat(2) {
                førteTil shouldBe "Endring"
            }
        }
    }
}
