package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mediator.IMessageMediator
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse
import java.util.UUID
import kotlin.test.Test

class FlyttBehandlingMottakTest {
    private val mediator = mockk<IMessageMediator>(relaxed = true)
    private val rapid =
        TestRapid().apply {
            FlyttBehandlingMottak(this, mediator)
        }

    @Test
    fun `tar imot flytt behandling`() {
        // language="JSON"
        rapid.sendTestMessage(
            """
            {
                "@event_name": "flytt_behandling",
                "ident": "12345678910",
                "behandlingId": "${UUID.randomUUID()}",
                "nyBasertPåId": "${UUID.randomUUID()}"
            }
            """.trimIndent(),
        )

        verify {
            mediator.behandle(any<FlyttBehandlingHendelse>(), any(), any())
        }
    }
}
