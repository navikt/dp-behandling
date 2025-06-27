package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.MessageMediator
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class SøknadInnsendtMottakTest {
    private val testRapid = TestRapid()
    private val messageMediator = mockk<MessageMediator>(relaxed = true)
    private val søknadInnsendtMottak = SøknadInnsendtMottak(testRapid, messageMediator)

    @Test
    fun `skal skippe søknad med fagsakId 0`() {
        testRapid.sendTestMessage(søknadInnsendMelding(0))
        verify(exactly = 0) {
            messageMediator.behandle(
                any(),
                any() as SøknadInnsendtMessage,
                any(),
            )
        }
    }

    @Test
    fun `skal ikke skippe søknad med fagsakId != 0`() {
        testRapid.sendTestMessage(søknadInnsendMelding(123))
        verify(exactly = 1) {
            messageMediator.behandle(
                any(),
                any() as SøknadInnsendtMessage,
                any(),
            )
        }
    }

    private fun søknadInnsendMelding(fagsakId: Int) =
        """{
          |   "@event_name": "søknad_behandlingsklar",
          |   "@id": "${UUID.randomUUID()}",
          |   "ident": "12345678910",
          |   "fagsakId": $fagsakId,
          |   "journalpostId": 1,
          |   "innsendt": "${LocalDateTime.now()}",
          |   "søknadId": "123e4567-e89b-12d3-a456-426614174000"
          |}
        """.trimMargin()
}
