package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.regelverk.HendelseMottaker
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SøknadInnsendtMottakTest {
    private val testRapid = TestRapid()
    private val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)
    private val søknadInnsendtMottak = SøknadInnsendtMottak(testRapid, hendelseMottaker)

    @Test
    fun `skal inkludere søknad med fagsakId 0`() {
        testRapid.sendTestMessage(søknadInnsendMelding(0))
        verify(exactly = 1) {
            hendelseMottaker.behandle(
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
            hendelseMottaker.behandle(
                any(),
                any() as SøknadInnsendtMessage,
                any(),
            )
        }
    }

    @Test
    fun `skal håndtere prodfeil-melding`() {
        testRapid.sendTestMessage(prodfeilJson)
        verify(exactly = 1) {
            hendelseMottaker.behandle(
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

    private val prodfeilJson =
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
}
