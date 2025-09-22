package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
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
    fun `tar imot søknad om ny og republiserer som behandlingsklar`() {
        rapid.sendTestMessage(innsendingJSON())
        rapid.inspektør.size shouldBe 1
        rapid.inspektør.key(0) shouldBe ident
        with(rapid.inspektør.message(0)) {
            this["ident"].asText() shouldBe ident
            this["fagsakId"].asInt() shouldBe 123
            this["journalpostId"].asInt() shouldBe 123
            this["søknadId"].asUUID() shouldBe søknadId
            this["type"].asText() shouldBe "NySøknad"
        }

        every {
            messageMediator.behandle(
                any<StartHendelse>(),
                any<SøknadInnsendtMessage>(),
                any<MessageContext>(),
            )
        }
    }

    @Test
    fun `tar imot søknad om gjenopptak og republiserer som behandlingsklar`() {
        rapid.sendTestMessage(innsendingJSON("Gjenopptak"))
        rapid.inspektør.size shouldBe 1
        rapid.inspektør.key(0) shouldBe ident

        with(rapid.inspektør.message(0)) {
            this["ident"].asText() shouldBe ident
            this["fagsakId"].asInt() shouldBe 123
            this["journalpostId"].asInt() shouldBe 123
            this["søknadId"].asUUID() shouldBe søknadId
            this["type"].asText() shouldBe "Gjenopptak"
        }

        every {
            messageMediator.behandle(
                any<StartHendelse>(),
                any<SøknadInnsendtMessage>(),
                any<MessageContext>(),
            )
        }
    }

    @Test
    fun `tar ikke imot innsendinger vi ikke vet om`() {
        rapid.sendTestMessage(innsendingJSON("Utdanning"))
        rapid.inspektør.size shouldBe 0
    }

    @Language("JSON")
    fun innsendingJSON(type: String = "NySøknad") =
        """
        {
          "@event_name": "innsending_ferdigstilt",
          "type": "$type",
          "fødselsnummer": "$ident",
          "fagsakId": "123",
          "søknadsData": {
            "søknad_uuid": "$søknadId"
          },
          "journalpostId": "123"
        }
        """.trimIndent()
}
