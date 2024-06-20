package no.nav.dagpenger.behandling.mediator.mottak

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringIkkeRelevantHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class AvklaringIkkeRelevantMottakTest {
    private val messageMediator = mockk<MessageMediator>(relaxed = true)
    private val testRapid =
        TestRapid().also {
            AvklaringIkkeRelevantMottak(it, messageMediator)
        }

    @Test
    fun `kan motta ikke relevante avklaringer`() {
        val hendelse = slot<AvklaringIkkeRelevantHendelse>()
        every { messageMediator.behandle(capture(hendelse), any(), any()) } just runs
        testRapid.sendTestMessage(avklaringAvklartMelding)
        hendelse.isCaptured shouldBe true
        hendelse.captured.behandlingId.toString() shouldBe "123e4567-e89b-12d3-a456-426614174000"
        hendelse.captured.avklaringId.toString() shouldBe "123e4567-e89b-12d3-a456-426614174000"
    }

    private val avklaringAvklartMelding =
        // language=JSON
        """
        {
            "@event_name": "AvklaringIkkeRelevant",
            "ident": "12345678910",
            "avklaringId": "123e4567-e89b-12d3-a456-426614174000",
            "kode": "AVKLARING_KODE",
            "behandlingId": "123e4567-e89b-12d3-a456-426614174000"
        }
        """.trimIndent()
}
