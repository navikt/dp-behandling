package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.spyk
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.modell.Behandling
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.UUID

class ArenaOppgaveMottakTest {
    @Test
    fun `Sender ikke avbryt-melding når vi ikke har noen behandling i saken`() {
        e2eTest {
            every {
                sakRepository.finnBehandling(15102351)
            } returns null

            rapid.sendTestMessage(meldingJson())

            rapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `Sender ikke avbryt-melding når vi behandlingen er ferdig`() {
        e2eTest {
            val behandlingId = UUID.randomUUID()
            every {
                sakRepository.finnBehandling(15102351)
            } returns SakRepository.Behandling("123", behandlingId, Behandling.TilstandType.Ferdig)

            rapid.sendTestMessage(meldingJson())

            rapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `Sender ikke avbryt-melding for oppgaver som ikke tildeles saksbehandler`() {
        e2eTest {
            val behandlingId = UUID.randomUUID()
            every {
                sakRepository.finnBehandling(15102351)
            } returns SakRepository.Behandling("123", behandlingId, Behandling.TilstandType.UnderBehandling)

            rapid.sendTestMessage(meldingJson())

            rapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `Sender avbryt-melding for oppgaver som tildeles saksbehandler`() {
        e2eTest {
            val behandlingId = UUID.randomUUID()
            every {
                sakRepository.finnBehandling(15102351)
            } returns SakRepository.Behandling("123", behandlingId, Behandling.TilstandType.UnderBehandling)

            rapid.sendTestMessage(meldingJson("ABC2024", "4450"))

            rapid.inspektør.size shouldBeExactly 1
            with(rapid.inspektør.message(0)) {
                this["@event_name"].asText() shouldBe "avbryt_behandling"
                this["ident"].asText() shouldBe "123"
                this["behandlingId"].asUUID() shouldBe behandlingId
            }

            // Samme melding kommer igjen, men nå er saken avbrutt
            every {
                sakRepository.finnBehandling(15102351)
            } returns SakRepository.Behandling("123", behandlingId, Behandling.TilstandType.Avbrutt)

            rapid.sendTestMessage(meldingJson("ABC2024", "ABC2024"))

            rapid.inspektør.size shouldBeExactly 1
        }
    }

    @Language("JSON")
    private fun meldingJson(
        endretAv: String = "ARBLINJE",
        endretAvFør: String = endretAv,
    ) = """{
      "table": "SIAMO.OPPGAVE_LOGG",
      "op_type": "U",
      "op_ts": "2024-11-05 12:12:46.000000",
      "current_ts": "2024-11-05 12:12:51.325004",
      "pos": "00000003570305543589",
      "before": {
        "USERNAME": "$endretAvFør"
      },
      "after": {
        "OPPGAVE_LOGG_ID": 238763568,
        "TRANS_ID": "148.27.871874",
        "TASK_ID": 267868027,
        "DESCRIPTION": "ABBA",
        "PRIORITY": 1,
        "DUEDATE": "2024-10-03 00:00:00",
        "USERNAME": "$endretAv",
        "KONTOR": "4416",
        "FYLKESNR": 50,
        "OPPRETTET_DATO": "2024-10-03 09:47:32",
        "ER_UTLAND": "N",
        "REG_DATO": "2024-10-03 09:47:32",
        "REG_USER": "ARBLINJE",
        "MOD_DATO": "2024-11-05 11:58:03",
        "MOD_USER": "ABC416",
        "OBJEKT": "SAK",
        "OBJEKT_ID": "15102351",
        "SAK_ID": 15102351,
        "SAK_TYPE": "DAGP",
        "VEDTAK_ID": null,
        "VEDTAK_TYPE": null,
        "OPPGAVETYPE_ID": "cx139.107.13.1874bfe6b:e639a26b28:-8000",
        "OPPGAVETYPE_BESKRIVELSE": "Start vedtaksbehandling",
        "GSAK_FAGOMRADE": "DAG",
        "GSAK_PRIORITET": "HOY_DAG",
        "OPERASJON": "DEL",
        "ENDRET_AV": "$endretAv",
        "TIDSPUNKT": "2024-11-05 12:12:45",
        "PERSON_ID": 3931114
      },
      "@id": "e0e25785-9332-4e2c-9a0f-9e85dfc82c1c",
      "@opprettet": "2024-11-05T12:12:51.351838572",
      "system_read_count": 0,
      "system_participating_services": [
        {
          "id": "e0e25785-9332-4e2c-9a0f-9e85dfc82c1c",
          "time": "2024-11-05T12:12:51.351838572",
          "service": "dp-behandling",
          "instance": "dp-behandling-5799cbb788-4x5lt",
          "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2024.11.05-11.10-c68b7eb"
        }
      ]
    }"""
}

private data class E2ETextContext(
    val sakRepository: SakRepositoryPostgres,
    val rapid: TestRapid,
)

private fun e2eTest(block: E2ETextContext.() -> Unit) {
    withMigratedDb {
        val sakRepository = spyk(SakRepositoryPostgres(dataSource))
        val rapid =
            TestRapid().apply {
                ArenaOppgaveMottak(this, sakRepository)
            }

        val testContext = E2ETextContext(sakRepository, rapid)
        block(testContext)
    }
}
