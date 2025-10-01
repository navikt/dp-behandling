package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.modell.hendelser.UtbetalingStatus
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class UtbetalingStatusMottakTest {
    private val testRapid = TestRapid()
    private val messageMediator = mockk<IMessageMediator>(relaxed = true)

    init {
        UtbetalingStatusMottak(rapidsConnection = testRapid, messageMediator)
    }

    @ParameterizedTest
    @ValueSource(strings = ["utbetaling_mottatt", "utbetaling_sendt", "utbetaling_feilet", "utbetaling_utført"])
    fun `vi kan motta utbetalingstatusmeldinger`(event: String) {
        val message = utbetalingStatusEvent(event)
        testRapid.sendTestMessage(message)
        val slot = slot<UtbetalingStatus>()
        verify(exactly = 1) { messageMediator.behandle(capture(slot), any<UtbetalingStatusMessage>(), any<MessageContext>()) }
        slot.isCaptured shouldBe true
        val hendelse = slot.captured
        hendelse.behandlingId shouldBe UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851")
        hendelse.ident() shouldBe "12345678901"
        hendelse.eksternMeldekortId.id shouldBe "654321"
    }

    private fun utbetalingStatusEvent(string: String) =
        //language=json
        """
        {
          "@event_name": "$string",
          "behandlingId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
          "sakId": "${UUID.randomUUID()}",
          "meldekortId": "654321",
          "ident": "12345678901",
          "status": "MOTTATT"
        }
        """.trimIndent()
}
