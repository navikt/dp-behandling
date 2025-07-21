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
}
