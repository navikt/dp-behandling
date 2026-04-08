package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.regel.hendelse.AvsluttetArbeidssokerperiodeHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AvsluttetArbeidssokerperiodeMottakTest {
    private val messageMediator = mockk<MessageMediator>(relaxed = true)
    private val rapid =
        TestRapid().apply {
            AvsluttetArbeidssokerperiodeMottak(this, messageMediator)
        }

    @Test
    fun `tar imot avsluttet arbeidssøkerperiode`() {
        rapid.sendTestMessage(
            avsluttetArbeidssokerperiodeMelding(
                ident = "12345678910",
                avsluttetTidspunkt = LocalDateTime.parse("2024-06-30T12:00:00"),
                fastsattMeldingsdag = LocalDate.parse("2024-06-25"),
            ),
        )

        val slot = slot<AvsluttetArbeidssokerperiodeHendelse>()
        verify {
            messageMediator.behandle(capture(slot), any<AvsluttetArbeidssokerperiodeMessage>(), any())
        }

        slot.captured.ident() shouldBe "12345678910"
        slot.captured.avsluttetTidspunkt shouldBe LocalDateTime.of(2024, 6, 30, 12, 0, 0)
        slot.captured.fastsattMeldingsdag shouldBe LocalDate.of(2024, 6, 25)
    }

    @Test
    fun `tar imot avsluttet arbeidssøkerperiode uten fastsattmeldingsdag`() {
        rapid.sendTestMessage(
            avsluttetArbeidssokerperiodeMelding(
                ident = "12345678910",
                avsluttetTidspunkt = LocalDateTime.parse("2024-06-30T12:00:00"),
            ),
        )

        val slot = slot<AvsluttetArbeidssokerperiodeHendelse>()
        verify {
            messageMediator.behandle(capture(slot), any<AvsluttetArbeidssokerperiodeMessage>(), any())
        }

        slot.captured.ident() shouldBe "12345678910"
        slot.captured.avsluttetTidspunkt shouldBe LocalDateTime.of(2024, 6, 30, 12, 0, 0)
    }

    private fun avsluttetArbeidssokerperiodeMelding(
        ident: String,
        avsluttetTidspunkt: LocalDateTime,
        fastsattMeldingsdag: LocalDate? = null,
    ) = JsonMessage
        .newMessage(
            "avsluttet_arbeidssokerperiode",
            buildMap {
                put("ident", ident)
                put("avsluttetTidspunkt", avsluttetTidspunkt)
                fastsattMeldingsdag?.let { put("fastsattMeldingsdag", it) }
            },
        ).toJson()
}
