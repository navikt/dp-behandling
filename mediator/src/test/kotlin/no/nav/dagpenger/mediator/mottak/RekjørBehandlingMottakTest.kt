package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mediator.MessageMediator
import no.nav.dagpenger.modell.hendelser.RekjørBehandlingHendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class RekjørBehandlingMottakTest {
    private val mediator = mockk<MessageMediator>(relaxed = true)

    private val rapid = TestRapid().also { RekjørBehandlingMottak(it, mediator, emptySet()) }

    @Test
    fun `tar imot rekjøringer`() {
        rapid.sendTestMessage(rekjørMelding)

        verify {
            mediator.behandle(any<RekjørBehandlingHendelse>(), any(), any())
        }
    }

    @Language("JSON")
    private val rekjørMelding =
        """
        {
            "@event_name": "rekjør_behandling",
            "ident": "12345678910",
            "behandlingId": "123e4567-e89b-12d3-a456-426614174000"
        }
        """.trimIndent()
}
