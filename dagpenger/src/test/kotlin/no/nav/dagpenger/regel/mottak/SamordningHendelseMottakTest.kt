package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.regel.mottak.SamordningHendelseMottak.SamordningHendelseMessage
import no.nav.dagpenger.regelverk.HendelseMottaker
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class SamordningHendelseMottakTest {
    private val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)
    private val rapid =
        TestRapid().also {
            SamordningHendelseMottak(it, hendelseMottaker)
        }

    @Test
    fun `lytter på meldinger fra RAMP`() {
        rapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    "annen_ytelse_endret",
                    mapOf(
                        "ident" to "123",
                        "tema" to "SYK",
                        "tidspunkt" to LocalDateTime.now(),
                    ),
                ).toJson(),
        )

        verify(exactly = 1) {
            hendelseMottaker.behandle(any<StartHendelse>(), any<SamordningHendelseMessage>(), any())
        }
    }

    @Test
    fun `leser ikke meldinger med tidspunkt som ikke er localdatetime`() {
        rapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    "annen_ytelse_endret",
                    mapOf(
                        "ident" to "123",
                        "tema" to "SYK",
                        "tidspunkt" to LocalDateTime.now().atOffset(ZoneOffset.UTC),
                    ),
                ).toJson(),
        )

        verify(exactly = 0) {
            hendelseMottaker.behandle(any<StartHendelse>(), any<SamordningHendelseMessage>(), any())
        }
    }
}
