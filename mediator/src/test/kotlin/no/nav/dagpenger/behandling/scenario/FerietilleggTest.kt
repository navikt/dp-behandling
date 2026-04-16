package no.nav.dagpenger.behandling.scenario

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.helpers.scenario.SimulertDagpengerSystem.Companion.nyttScenario
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.regel.FerietilleggBeløp
import no.nav.dagpenger.regel.KravPåFerietillegg
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test

class FerietilleggTest {
    @Test
    fun `tester ferietillegg`() {
        val fnr = "12345678901"
        val ferietilleggId = UUIDv7.ny()
        val opptjeningsår = 2018
        nyttScenario {
            ident = fnr
            inntektSiste12Mnd = 500000
        }.test {
            person.søkDagpenger(21.juni(2018))
            behovsløsere.løsTilForslag()

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            sendFerietillegg(fnr, ferietilleggId, opptjeningsår)
            behovsløsere.løsTilForslag()

            println("hei")
            behandlingsresultat {
//                rettighetsperioder.single().harRett shouldBe false
//                rettighetsperioder.single().fraOgMed shouldBe 21.juni(2018)

                opplysninger(FerietilleggBeløp.ferietilleggBeløp).single().verdi.verdi shouldBe 47500
                opplysninger(KravPåFerietillegg.harKravpåFerietillegg).single().verdi.verdi shouldBe true

//                opplysninger shouldHaveSize 22
            }
        }
    }
}
