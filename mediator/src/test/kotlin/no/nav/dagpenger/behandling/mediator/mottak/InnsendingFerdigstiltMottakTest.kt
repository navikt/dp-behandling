package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

internal class InnsendingFerdigstiltMottakTest {
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
        println(søknad())
        rapid.sendTestMessage(søknad())
        rapid.inspektør.size shouldBe 1
        rapid.inspektør.key(0) shouldBe ident
        val message = rapid.inspektør.message(0)
        with(message) {
            this["ident"].asText() shouldBe ident
            this["fagsakId"].asInt() shouldBe 123
            this["journalpostId"].asInt() shouldBe 123
            this["søknadId"].asUUID() shouldBe søknadId
            this["type"].asText() shouldBe "NySøknad"
        }

        rapid.sendTestMessage(message.toString())

        verify {
            messageMediator.behandle(
                any<StartHendelse>(),
                any<SøknadInnsendtMessage>(),
                any<MessageContext>(),
            )
        }
    }

    @Test
    fun `d prodfeil`() {
//        rapid.sendTestMessage(søknad())
//        rapid.inspektør.size shouldBe 1
//        rapid.inspektør.key(0) shouldBe ident
//        val message = rapid.inspektør.message(0)
//        with(message) {
//            this["ident"].asText() shouldBe ident
//            this["fagsakId"].asInt() shouldBe 123
//            this["journalpostId"].asInt() shouldBe 123
//            this["søknadId"].asUUID() shouldBe søknadId
//            this["type"].asText() shouldBe "NySøknad"
//        }

        rapid.sendTestMessage(feilJson)

        verify {
            messageMediator.behandle(
                any<StartHendelse>(),
                any<SøknadInnsendtMessage>(),
                any<MessageContext>(),
            )
        }
    }

    @Test
    fun `tar imot søknad om gjenopptak og republiserer som behandlingsklar`() {
        rapid.sendTestMessage(søknad(type = "Gjenopptak", fagsakId = null))
        rapid.inspektør.size shouldBe 1
        rapid.inspektør.key(0) shouldBe ident

        val message = rapid.inspektør.message(0)
        with(message) {
            this["ident"].asText() shouldBe ident
            this["fagsakId"].asInt() shouldBe 0
            this["journalpostId"].asInt() shouldBe 123
            this["søknadId"].asUUID() shouldBe søknadId
            this["type"].asText() shouldBe "Gjenopptak"
        }

        rapid.sendTestMessage(message.toString())
        verify {
            messageMediator.behandle(
                any<StartHendelse>(),
                any<SøknadInnsendtMessage>(),
                any<MessageContext>(),
            )
        }
    }

    @Test
    fun `tar ikke imot innsendinger vi ikke vet om`() {
        rapid.sendTestMessage(søknad("Utdanning"))
        rapid.inspektør.size shouldBe 0
    }

    val feilJson =
        """
        {
          "@event_name": "søknad_behandlingsklar",
          "ident": "12345678901",
          "søknadId": "d0ae0c67-b0b4-42fa-ae06-c2389a7b85d2",
          "fagsakId": 0,
          "innsendt": "2026-02-13T11:03:24",
          "journalpostId": 740596204,
          "type": "NySøknad",
          "@id": "eef558ef-6e83-4078-900e-fe717e2d0c28",
          "@opprettet": "2026-02-13T11:03:33.701592004",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "id": "eef558ef-6e83-4078-900e-fe717e2d0c28",
              "time": "2026-02-13T11:03:33.701592004",
              "service": "dp-behandling",
              "instance": "dp-behandling-db6468cf5-mpflm",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2026.02.12-08.34-5b6d5b4"
            },
            {
              "id": "eef558ef-6e83-4078-900e-fe717e2d0c28",
              "time": "2026-02-13T11:03:33.709220759",
              "service": "dp-behandling",
              "instance": "dp-behandling-db6468cf5-mpflm",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2026.02.12-08.34-5b6d5b4"
            }
          ],
          "@forårsaket_av": {
            "id": "8d25858c-3376-45f2-a859-b498673cbeed",
            "opprettet": "2026-02-13T11:03:33.698233325",
            "event_name": "innsending_ferdigstilt"
          }
        }
        """.trimIndent()

    fun søknad(
        type: String = "NySøknad",
        fagsakId: Int? = 123,
    ) = buildMap {
        put("type", type)
        if (fagsakId != null) put("fagsakId", fagsakId)
        put("fødselsnummer", ident)
        put("søknadsData", mapOf("søknad_uuid" to søknadId))
        put("datoRegistrert", "2024-06-01T12:00:00")
        put("journalpostId", "123")
    }.let { JsonMessage.newMessage("innsending_ferdigstilt", it).toJson() }
}
