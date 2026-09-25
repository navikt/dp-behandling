package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.mediator.IMessageMediator
import no.nav.dagpenger.mediator.MessageMediator
import no.nav.dagpenger.mediator.repository.MeldekortRepository
import no.nav.dagpenger.modell.hendelser.Meldekort
import no.nav.dagpenger.modell.hendelser.MeldekortId
import no.nav.dagpenger.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.opplysning.Systemaktør
import no.nav.dagpenger.regel.hendelse.BeregnMeldekortHendelse
import no.nav.dagpenger.regel.hendelse.OmgjøringHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KafkaStartHendelseOppretterTest {
    @Test
    fun `omgjøring har dp-sak som oppretter`() {
        val rapid = TestRapid()
        val mediator = mockk<IMessageMediator>(relaxed = true)
        OmgjøringMottak(rapid, mediator, mockk())

        rapid.sendTestMessage(
            """
            {
              "@event_name": "omgjør_behandling",
              "ident": "12345678910",
              "gjelderDato": "2026-09-21"
            }
            """.trimIndent(),
        )

        val hendelse = slot<OmgjøringHendelse>()
        verify(exactly = 1) {
            mediator.behandle(capture(hendelse), any(), any())
        }
        hendelse.captured.opprettetAv shouldBe dpSak
    }

    @Test
    fun `beregning av meldekort har dp-sak som oppretter`() {
        val meldekortId = UUID.randomUUID()
        val meldekort =
            Meldekort(
                id = meldekortId,
                meldingsreferanseId = UUID.randomUUID(),
                ident = "12345678910",
                eksternMeldekortId = MeldekortId("1"),
                fom = LocalDate.of(2026, 9, 7),
                tom = LocalDate.of(2026, 9, 20),
                kilde = MeldekortKilde("Bruker", "12345678910"),
                dager = emptyList(),
                innsendtTidspunkt = LocalDateTime.of(2026, 9, 20, 12, 0),
                korrigeringAv = null,
                meldedato = LocalDate.of(2026, 9, 20),
                kanSendesFra = LocalDate.of(2026, 9, 20),
            )
        val rapid = TestRapid()
        val mediator = mockk<MessageMediator>(relaxed = true)
        val meldekortRepository = mockk<MeldekortRepository>()
        every { meldekortRepository.hent(meldekortId) } returns meldekort
        BeregnMeldekortMottak(rapid, mediator, meldekortRepository)

        rapid.sendTestMessage(
            """
            {
              "@event_name": "beregn_meldekort",
              "ident": "12345678910",
              "meldekortId": "$meldekortId"
            }
            """.trimIndent(),
        )

        val hendelse = slot<BeregnMeldekortHendelse>()
        verify(exactly = 1) {
            mediator.behandle(capture(hendelse), any(), any())
        }
        hendelse.captured.opprettetAv shouldBe dpSak
    }

    private companion object {
        val dpSak = Systemaktør.dpSak
    }
}
