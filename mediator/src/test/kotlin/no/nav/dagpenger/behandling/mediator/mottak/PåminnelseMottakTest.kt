package no.nav.dagpenger.behandling.mediator.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.modell.hendelser.PåminnelseHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class PåminnelseMottakTest {
    private val mediator = mockk<MessageMediator>(relaxed = true)

    private val rapid = TestRapid().also { PåminnelseMottak(it, mediator) }

    @Test
    fun `tar imot påminnelser`() {
        rapid.sendTestMessage(påminnelse)

        verify {
            mediator.behandle(any<PåminnelseHendelse>(), any(), any())
        }
    }

    @Language("JSON")
    private val påminnelse =
        """
        {
            "@event_name": "behandling_står_fast",
            "ident": "12345678910",
            "behandlingId": "123e4567-e89b-12d3-a456-426614174000"
        }
        """.trimIndent()
}
