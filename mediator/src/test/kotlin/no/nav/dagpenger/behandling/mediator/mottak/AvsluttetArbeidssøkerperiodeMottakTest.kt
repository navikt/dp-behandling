package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.mottak.AvsluttetArbeidssøkerperiodeMottak.AvsluttetArbeidssøkerperiodeMessage
import no.nav.dagpenger.regel.hendelse.AvsluttetArbeidssøkerperiodeHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvsluttetArbeidssøkerperiodeMottakTest {
    private val rapid = TestRapid()
    private val mediator = mockk<IMessageMediator>(relaxed = true)

    @Test
    fun `lytter på meldinger fra RAMP`() {
        AvsluttetArbeidssøkerperiodeMottak(rapid, mediator)

        val melding =
            JsonMessage.newMessage(
                "utmeldt_fra_arbeidssøkerregisteret",
                mapOf(
                    "ident" to "12312312311",
                    "periodeId" to UUID.randomUUID(),
                    "fastsattMeldedato" to LocalDate.now(),
                    "avregistrertTidspunkt" to LocalDateTime.now(),
                    "årsak" to "UTMELDT_I_ASR",
                ),
            )

        rapid.sendTestMessage(melding.toJson())

        verify {
            mediator.behandle(any<AvsluttetArbeidssøkerperiodeHendelse>(), any<AvsluttetArbeidssøkerperiodeMessage>(), any())
        }
    }
}
