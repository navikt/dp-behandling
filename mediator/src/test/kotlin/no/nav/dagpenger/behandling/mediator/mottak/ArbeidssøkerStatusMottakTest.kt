package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.regel.ArbeidssøkerstatusAvsluttet
import org.junit.jupiter.api.Test

class ArbeidssøkerStatusMottakTest {
    private val testRapid = TestRapid()
    private val messageMediator = mockk<MessageMediator>(relaxed = true)

    init {
        ArbeidssøkerstatusAvsluttetMottak(testRapid, messageMediator)
    }

    @Test
    fun `skal lytte på arbeidssøkerstatus_endret eventer`() {
        val melding = arbeidssøkerstatus_endret()
        testRapid.sendTestMessage(melding)
        verify {
            messageMediator.behandle(
                any<ArbeidssøkerstatusAvsluttet>(),
                any<ArbeidssøkerstatusAvsluttetMottak.ArbeidssøkerstatusAvsluttetMessage>(),
                any<MessageContext>(),
            )
        }
    }

    private fun arbeidssøkerstatus_endret() =
        //language=JSON
        """
        {
          "@event_name" : "arbeidssøkerstatus_endret",
          "fom" : "2025-04-28T12:51:49.01",        
          "ident" : "12345678910",
          "periodeId" : "08ed09fb-0d91-4b24-a8f3-f695ea0c220b",
          "@kilde" : {
            "data" : {
              "id" : "08ed09fb-0d91-4b24-a8f3-f695ea0c220b",
              "identitetsnummer" : "12345678910",
              "startet" : {
                "tidspunkt" : "2025-04-28T10:51:49.010Z",
                "utfoertAv" : {
                  "type" : "SLUTTBRUKER",
                  "id" : "12345678910",
                  "sikkerhetsnivaa" : "vely sikker"
                },
                "kilde" : "arbeidssøkerstatus",
                "aarsak" : "1.0",
                "tidspunktFraKilde" : {
                  "tidspunkt" : "2025-04-28T10:51:49.061Z",
                  "avviksType" : "RETTING"
                }
              },
              "avsluttet" : {
                "tidspunkt" : "2025-04-28T10:51:49.010Z",
                "utfoertAv" : {
                  "type" : "SLUTTBRUKER",
                  "id" : "12345678910",
                  "sikkerhetsnivaa" : "vely sikker"
                },
                "kilde" : "arbeidssøkerstatus",
                "aarsak" : "1.0",
                "tidspunktFraKilde" : {
                  "tidspunkt" : "2025-04-28T10:51:49.061Z",
                  "avviksType" : "RETTING"
                }
              }
            }
          },
          "tom" : "2025-04-28T12:51:49.01",
          "avsluttetAv" : "sluttbruker",
          "@id" : "05a97c9d-f0bb-4d47-9c6b-a9413a754b3b",
          "@opprettet" : "2025-04-28T12:51:49.106257",
          "system_read_count" : 0,
          "system_participating_services" : [ {
            "id" : "05a97c9d-f0bb-4d47-9c6b-a9413a754b3b",
            "time" : "2025-04-28T12:51:49.106257"
          } ]
        }
        """.trimIndent()
}
