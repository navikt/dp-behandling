package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKorrigeringHendelse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours

class MeldekortKorrigeringMottakTest {
    private val rapid = TestRapid()
    private val messageMediator = mockk<MessageMediator>(relaxed = true)

    init {
        MeldekortKorrigeringMottak(rapid, messageMediator)
    }

    @BeforeEach
    fun setup() {
        rapid.reset()
        clearMocks(messageMediator)
    }

    @Test
    fun `vi kan ta i mot korrigering av et meldekort`() {
        rapid.sendTestMessage(korrigertMeldekortJson)
        val hendelse = slot<MeldekortKorrigeringHendelse>()

        verify {
            messageMediator.behandle(capture(hendelse), any<MeldekortKorrigeringMessage>(), any())
        }

        hendelse.isCaptured shouldBe true
        hendelse.captured.ident() shouldBe "12345123451"
        hendelse.captured.meldekortId shouldBe 1001
        hendelse.captured.orginalMeldekortId shouldBe 1000
        hendelse.captured.fom shouldBe LocalDate.of(2025, 1, 20)
        hendelse.captured.tom shouldBe LocalDate.of(2025, 2, 2)
        hendelse.captured.dager.size shouldBe 14
        hendelse.captured.dager
            .first()
            .dato shouldBe LocalDate.of(2025, 1, 20)
        hendelse.captured.dager
            .first()
            .aktiviteter.size shouldBe 1
        hendelse.captured.dager
            .first()
            .aktiviteter
            .first()
            .type shouldBe AktivitetType.Arbeid
        hendelse.captured.dager
            .first()
            .aktiviteter
            .first()
            .timer shouldBe 5.hours
    }
}

// language=json
private val korrigertMeldekortJson =
    """
    {
      "@event_name": "meldekort_korrigert",
      "ident": "12345123451",
      "id": 1001,
      "orginalId": 1000,
      "periode": { 
          "fraOgMed": "2025-01-20",
          "tilOgMed": "2025-02-02"
      },
      "kilde": {
        "rolle": "Bruker",
        "ident": "12345123451"
      },
      "mottattDato": "2025-02-02",
      "dager": [
        {
          "dato": "2025-01-20",
          "meldt" : true, 
          "dagIndex": 1,
          "aktiviteter": [
            {
              "type": "Arbeid",
              "timer": "PT5H"
            }
          ]
        },
        {
          "dato": "2025-01-21",
          "meldt" : true, 
          "dagIndex": 2,
          "aktiviteter": []
        },
        {
          "dato": "2025-01-22",
          "meldt" : true, 
          "dagIndex": 3,
          "aktiviteter": [
            {
              "type": "Fravaer"
            }
          ]
        },
        {
          "dato": "2025-01-23",
          "meldt" : true, 
          "dagIndex": 4,
          "aktiviteter": [
            {
              "type": "Syk"
            }
          ]
        },
        {
          "dato": "2025-01-24",
          "meldt" : true, 
          "dagIndex": 5,
          "aktiviteter": [
            {
              "type": "Arbeid",
              "timer": "PT2H"
            }
          ]
        },
        {
          "dato": "2025-01-25",
          "meldt" : true, 
          "dagIndex": 6,
          "aktiviteter": []
        },
        {
          "dato": "2025-01-26",
          "meldt" : true, 
          "dagIndex": 7,
          "aktiviteter": []
        },
        {
          "dato": "2025-01-27",
          "meldt" : true, 
          "dagIndex": 8,
          "aktiviteter": []
        },
        {
          "dato": "2025-01-28",
          "meldt" : true, 
          "dagIndex": 9,
          "aktiviteter": []
        },
        {
          "dato": "2025-01-29",
          "meldt" : true, 
          "dagIndex": 10,
          "aktiviteter": []
        },
        {
          "dato": "2025-01-30",
          "meldt" : true, 
          "dagIndex": 11,
          "aktiviteter": []
        },
        {
          "dato": "2025-01-31",
          "meldt" : true, 
          "dagIndex": 12,
          "aktiviteter": []
        },
        {
          "dato": "2025-02-01",
          "meldt" : true, 
          "dagIndex": 13,
          "aktiviteter": []
        },
        {
          "dato": "2025-02-02",
          "meldt" : true, 
          "dagIndex": 14,
          "aktiviteter": []
        }
      ],
      "@id": "c1e95eca-cc53-4c58-aa16-957f1e623f74",
      "@opprettet": "2023-06-12T08:40:44.544584",
      "system_read_count": 0,
      "system_participating_services": [
        {
          "id": "c1e95eca-cc53-4c58-aa16-957f1e623f74",
          "time": "2023-06-12T08:40:44.544584"
        }
      ]
    }
    """.trimIndent()
