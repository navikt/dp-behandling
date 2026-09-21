package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.modell.Oppretter
import no.nav.dagpenger.regel.hendelse.OpprettBehandlingHendelse
import no.nav.dagpenger.regelverk.HendelseMottaker
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpprettBehandlingMottakTest {
    @Test
    fun `Kafka-opprettet starthendelse har dp-sak som oppretter`() {
        val rapid = TestRapid()
        val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)
        OpprettBehandlingMottak(rapid, hendelseMottaker)

        rapid.sendTestMessage(
            """
            {
              "@event_name": "opprett_behandling",
              "ident": "12345678910",
              "prøvingsdato": "${LocalDate.of(2026, 9, 21)}",
              "begrunnelse": "Test"
            }
            """.trimIndent(),
        )

        val hendelse = slot<OpprettBehandlingHendelse>()
        verify(exactly = 1) {
            hendelseMottaker.behandle(capture(hendelse), any(), any())
        }
        hendelse.captured.opprettetAv shouldBe Oppretter(Oppretter.Type.System, "dp-sak")
    }
}
