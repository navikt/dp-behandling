package no.nav.dagpenger.scenario

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.ferietillegg.FerietilleggBeløp
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg
import no.nav.dagpenger.mediator.juni
import no.nav.dagpenger.scenario.SimulertDagpengerSystem.Companion.nyttScenario
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
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            behandlingsresultat {
                opplysninger(FerietilleggBeløp.ferietilleggBeløp).single().verdi.verdi shouldBe 47500
                opplysninger(KravPåFerietillegg.harKravpåFerietillegg).single().verdi.verdi shouldBe true
            }
        }
    }

    @Test
    fun `tester kjeding av ferietillegg`() {
        val fnr = "12345678901"
        val ferietilleggId = UUIDv7.ny()
        val opptjeningsår = 2018
        nyttScenario {
            ident = fnr
            inntektSiste12Mnd = 500000
        }.test {
            // vi trenger at personen har en innvilget søknad om dagpenger
            person.søkDagpenger(21.juni(2018))
            val dagpengerBehandlingId = person.behandlingId
            behovsløsere.løsTilForslag()
            person.behandling.basertPå shouldBe null

            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()
            saksbehandler.beslutt()

            // Vi lager en ferietillegg behandling som bruker den første behandlingen for beregning men kjeder seg ikke på den
            sendFerietillegg(fnr, ferietilleggId, opptjeningsår)
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            val ferietilleggBehandlingId = person.behandlingId

            behandlingsresultat {
                basertPå shouldBe null
            }

            // Nå har vi to kjeder, en med vanlig dagpenger og en ferietillegg
            // Vi sender inn et meldekort som skal kjede seg på dagpenger behandlingen
            person.sendInnMeldekort(1)
            meldekortBatch(true)

            val meldekortBehandlingId = person.behandlingId
            person.behandling.basertPå shouldBe dagpengerBehandlingId

            // Vi lager en ferietillegg behandling til som baserer seg på den forrige ferietillegg
            sendFerietillegg(fnr, UUIDv7.ny(), opptjeningsår)
            behovsløsere.løsTilForslag()
            saksbehandler.lukkAlleAvklaringer()
            saksbehandler.godkjenn()

            val ferietilleggBehandling2Id = person.behandlingId
            println("dagpengerId : $dagpengerBehandlingId")
            println("ferietilleggId : $ferietilleggId")
            println("meldekortId : $meldekortBehandlingId")
            println("ferietillegg2Id : ${person.behandlingId}")
            println("ferietillegg2Id : $ferietilleggBehandling2Id")

            behandlingsresultat {
                println("basertPå : $basertPå")
                println("id : $behandlingId")
                basertPå shouldBe ferietilleggBehandlingId
            }
        }
    }
}
