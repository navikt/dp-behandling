package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.mottak.SamordningHendelseMottak.SamordningHendelseMessage
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class SamordningHendelseMottakTest {
    private val mediator = mockk<IMessageMediator>(relaxed = true)
    private val rapid =
        TestRapid().also {
            SamordningHendelseMottak(it, mediator)
        }

    @Test
    fun `leser ikke meldinger som matcher filteret`() {
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
            mediator.behandle(any<StartHendelse>(), any<SamordningHendelseMessage>(), any())
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
            mediator.behandle(any<StartHendelse>(), any<SamordningHendelseMessage>(), any())
        }
    }
}
