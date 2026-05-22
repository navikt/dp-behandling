package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.regel.hendelse.AvsluttetArbeidssøkerperiodeHendelse
import no.nav.dagpenger.regel.mottak.AvsluttetArbeidssøkerperiodeMottak.AvsluttetArbeidssøkerperiodeMessage
import no.nav.dagpenger.regelverk.HendelseMottaker
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvsluttetArbeidssøkerperiodeMottakTest {
    private val rapid = TestRapid()
    private val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)

    @Test
    fun `lytter på meldinger fra RAMP`() {
        AvsluttetArbeidssøkerperiodeMottak(rapid, hendelseMottaker)

        val melding =
            JsonMessage.newMessage(
                "avsluttet_arbeidssokerperiode",
                mapOf(
                    "ident" to "12312312311",
                    "periodeId" to UUID.randomUUID(),
                    "fastsattMeldedato" to LocalDate.now(),
                    "avregistrertTidspunkt" to LocalDateTime.now(),
                    "årsak" to "UTMELDT_I_ARBEIDSSØKERREGISTERET",
                ),
            )

        rapid.sendTestMessage(melding.toJson())

        verify {
            hendelseMottaker.behandle(any<AvsluttetArbeidssøkerperiodeHendelse>(), any<AvsluttetArbeidssøkerperiodeMessage>(), any())
        }
    }
}
