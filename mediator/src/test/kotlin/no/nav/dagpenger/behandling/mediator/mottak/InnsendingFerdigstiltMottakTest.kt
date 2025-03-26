package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Regelverkstype
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.UUID

class InnsendingFerdigstiltMottakTest {
    private val messageMediator = mockk<MessageMediator>(relaxed = true)

    private val rapid =
        TestRapid().also {
            InnsendingFerdigstiltMottak(it)
            SøknadInnsendtMottak(it, messageMediator)
        }
    private val ident = "123123123"
    private val søknadId by lazy { UUID.randomUUID() }

    @Test
    fun `tar imot innsending og republiserer som behandlingsklar`() {
        rapid.sendTestMessage(innsendingJSON)
        rapid.inspektør.size shouldBe 1
        rapid.inspektør.key(0) shouldBe ident
        with(rapid.inspektør.message(0)) {
            this["ident"].asText() shouldBe ident
            this["fagsakId"].asInt() shouldBe 123
            this["journalpostId"].asInt() shouldBe 123
            this["søknadId"].asUUID() shouldBe søknadId
        }

        every {
            messageMediator.behandle(
                any<StartHendelse<Regelverkstype>>(),
                any<SøknadInnsendtMessage>(),
                any<MessageContext>(),
            )
        }
    }

    @Language("JSON")
    val innsendingJSON =
        """
        {
          "@event_name": "innsending_ferdigstilt",
          "type": "NySøknad",
          "fødselsnummer": "$ident",
          "fagsakId": "123",
          "søknadsData": {
            "søknad_uuid": "$søknadId"
          },
          "journalpostId": "123"
        }
        """.trimIndent()
}
