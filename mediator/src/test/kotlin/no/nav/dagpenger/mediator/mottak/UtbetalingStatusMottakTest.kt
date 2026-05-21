package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mediator.IMessageMediator
import no.nav.dagpenger.modell.hendelser.UtbetalingStatus
import org.junit.jupiter.api.Test
import java.util.UUID

class UtbetalingStatusMottakTest {
    private val testRapid = TestRapid()
    private val messageMediator = mockk<IMessageMediator>(relaxed = true)

    init {
        UtbetalingStatusMottak(rapidsConnection = testRapid, messageMediator)
    }

    @Test
    fun `vi kan motta utbetalingstatusmeldinger`() {
        val slots = mutableListOf<UtbetalingStatus>()
        listOf("utbetaling_mottatt", "utbetaling_sendt", "utbetaling_feilet", "utbetaling_utført").forEach { event ->
            val message = utbetalingStatusEvent(event)
            testRapid.sendTestMessage(message)
        }
        verify(exactly = 4) { messageMediator.behandle(capture(slots), any<UtbetalingStatusMessage>(), any<MessageContext>()) }
        slots.forEach { hendelse ->
            hendelse.behandlingId shouldBe UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851")
            hendelse.ident() shouldBe "12345678901"
            hendelse.behandletHendelseId shouldBe "654321"
        }
    }

    private fun utbetalingStatusEvent(string: String) =
        //language=json
        """
        {
          "@event_name": "$string",
          "behandlingId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
          "sakId": "${UUID.randomUUID()}",
          "behandletHendelseId": "654321",
          "ident": "12345678901",
          "status": "MOTTATT"
        }
        """.trimIndent()
}
