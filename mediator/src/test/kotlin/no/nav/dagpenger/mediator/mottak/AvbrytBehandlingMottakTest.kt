package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.mediator.MessageMediator
import no.nav.dagpenger.modell.hendelser.AvbrytBehandlingHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class AvbrytBehandlingMottakTest {
    private val messageMediator = mockk<MessageMediator>(relaxed = true)
    private val rapid =
        TestRapid().apply {
            AvbrytBehandlingMottak(this, messageMediator)
        }

    @Test
    fun `avbryter behandlinger med årsak`() {
        val behandlingId = UUID.randomUUID()
        val årsak = "Feil i inntekter for vurdering av minsteinntekt"
        rapid.sendTestMessage(getAvbrytBehandlingMelding(behandlingId, "123", årsak))

        val slot = slot<AvbrytBehandlingHendelse>()
        verify {
            messageMediator.behandle(capture(slot), any(), any())
        }

        slot.captured.behandlingId shouldBe behandlingId
        slot.captured.årsak shouldBe årsak
    }

    private fun getAvbrytBehandlingMelding(
        behandlingId: UUID,
        ident: String,
        årsak: String,
    ) = JsonMessage
        .newMessage(
            "avbryt_behandling",
            mapOf(
                "behandlingId" to behandlingId.toString(),
                "ident" to ident,
                "årsak" to årsak,
            ),
        ).toJson()
}
