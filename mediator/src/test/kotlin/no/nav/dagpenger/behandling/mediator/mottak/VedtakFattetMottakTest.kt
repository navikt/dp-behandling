package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import org.junit.jupiter.api.Test

class VedtakFattetMottakTest {
    private val rapid = TestRapid()
    private val meldekortRepository = mockk<MeldekortRepository>(relaxed = true)

    init {
        VedtakFattetMottak(rapid, meldekortRepository)
    }

    @Test
    fun `mottar vedtak`() {
        val vedtak =
            // language=JSON
            """
            {
                "@event_name": "vedtak_fattet",
                "behandlingId": "12345678-1234-1234-1234-123456789012",
                "behandletHendelse": {
                    "type": "Meldekort",
                    "id": 1
                }
            }
            """.trimIndent()

        rapid.sendTestMessage(vedtak)

        verify { meldekortRepository.markerSomFerdig(MeldekortId("1")) }
    }

    @Test
    fun `mottar behandling avbrutt`() {
        val vedtak =
            // language=JSON
            """
            {
              "@event_name": "behandling_avbrutt",
              "ident": "11109233444",
              "behandlingId": "01995c57-a424-7983-a9fe-58c4b8469e3a",
              "behandletHendelse": {
                "id": "1",
                "datatype": "String",
                "type": "Meldekort"
              },
              "årsak": "Avbrutt av datamaskinen",
              "@id": "0fb911a2-8ff6-43da-ab13-46abf2cf181f",
              "@opprettet": "2025-09-18T12:21:11.92664"
            }
            """.trimIndent()

        rapid.sendTestMessage(vedtak)

        verify { meldekortRepository.markerSomFerdig(MeldekortId("1")) }
    }
}
