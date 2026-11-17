package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.modell.hendelser.MigreringId
import no.nav.dagpenger.regel.DagpengerRegistrering
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class MigreringJsonFlytTest {
    private val jsonMapper = JsonMapper.builder().build()
    private val migreringId = UUIDv7.ny()
    private val rapid = TestRapid()
    private val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)

    @Test
    fun `etterspør opplysninger i flere JSON-runder før migreringen starter behandling`() {
        MigrerYtelseMottak(rapid, hendelseMottaker, DagpengerRegistrering().opplysningstyper)
        val startet = slot<MigrerYtelseHendelse>()
        var melding = startmelding()
        jsonMapper.readTree(melding).has("opplysninger") shouldBe false

        for (runde in 1..10) {
            rapid.reset()
            rapid.sendTestMessage(melding)

            if (rapid.inspektør.size == 0) {
                verify(exactly = 1) { hendelseMottaker.behandle(capture(startet), any(), any()) }
                (runde > 2) shouldBe true
                startet.captured.eksternId shouldBe MigreringId(migreringId)
                val rapport =
                    startet.captured.forretningsprosess
                        .regelkjøring(startet.captured.lagFaktagrunnlag())
                        .evaluer()
                rapport.erFerdig() shouldBe true
                rapport.mangler shouldBe emptyList()
                return
            }

            verify(exactly = 0) { hendelseMottaker.behandle(any(), any(), any()) }
            rapid.inspektør.size shouldBe 1
            val svar = rapid.inspektør.message(0)
            withClue("Runde $runde") {
                svar["@event_name"].asString() shouldBe "migrering_manglende_opplysninger"
            }
            svar["migreringId"].asString() shouldBe migreringId.toString()
            svar["sakId"].asInt() shouldBe 13699879
            svar.has("årsak") shouldBe false
            svar.has("simulerteOpplysninger") shouldBe false
            svar.path("opplysninger") shouldBe jsonMapper.readTree(melding).path("opplysninger")

            val manglende = svar["manglendeOpplysninger"].values().map { it["uuid"].asString() }
            manglende.isNotEmpty() shouldBe true
            manglende.distinct() shouldBe manglende
            val tidligere =
                svar
                    .path("opplysninger")
                    .takeIf { it.isArray }
                    ?.values()
                    ?.toList()
                    .orEmpty()
            val tidligereTyper = tidligere.map { it["uuid"].asString() }
            manglende.none { it in tidligereTyper } shouldBe true
            val nye = manglende.map { tilgjengeligeOpplysninger.getValue(it) }

            melding = opplysningsmelding(jsonMapper.writeValueAsString(tidligere + nye))
            val neste = jsonMapper.readTree(melding)
            neste["@event_name"].asString() shouldBe "migrering_opplysninger"
            neste.has("manglendeOpplysninger") shouldBe false
            neste["opplysninger"].values().map { it["uuid"].asString() } shouldBe tidligereTyper + manglende
        }

        error("Migreringen ble ikke ferdig etter ti meldingsrunder")
    }

    private fun startmelding() =
        """
        {
          "@event_name": "migrering_startet",
          "@id": "${UUIDv7.ny()}",
          "@opprettet": "2026-01-05T12:00:00",
          "migreringId": "$migreringId",
          "sakId": 13699879,
          "ident": "12345678910",
          "migreringTidspunkt": "2026-01-05"
        }
        """.trimIndent()

    private fun opplysningsmelding(opplysninger: String) =
        """
        {
          "@event_name": "migrering_opplysninger",
          "@id": "${UUIDv7.ny()}",
          "@opprettet": "2026-01-05T12:00:00",
          "migreringId": "$migreringId",
          "sakId": 13699879,
          "ident": "12345678910",
          "migreringTidspunkt": "2026-01-05",
          "opplysninger": $opplysninger
        }
        """.trimIndent()

    private val tilgjengeligeOpplysninger =
        jsonMapper
            .readTree(
                """
                [
                  {"uuid": "0194881f-940b-76ff-acf5-ba7bcb367233", "verdi": "1990-01-01"},
                  {"uuid": "01992934-66e4-7606-bdd3-c6c9dd420ffd", "verdi": 0},
                  {"uuid": "01992956-e349-76b1-8f68-c9d481df3a32", "verdi": 520},
                  {"uuid": "01997b70-a12c-7622-bff8-82a20687e640", "verdi": 0},
                  {
                    "uuid": "0194881f-9413-77ce-92ec-d29700f04244",
                    "verdi": {
                      "inntektsId": "0194881f-9413-77ce-92ec-d29700f04245",
                      "sisteAvsluttendeKalenderMåned": "2025-11",
                      "inntektsListe": [
                        {
                          "årMåned": "2025-11",
                          "klassifiserteInntekter": [
                            {"beløp": 600000, "inntektKlasse": "ARBEIDSINNTEKT"}
                          ],
                          "harAvvik": false
                        }
                      ]
                    }
                  },
                  {"uuid": "0194881f-9442-707b-a6ee-e96c06877be0", "verdi": true},
                  {"uuid": "0194881f-9444-7a73-a458-0af81c034d85", "verdi": true},
                  {"uuid": "0194881f-9444-7a73-a458-0af81c034d86", "verdi": false},
                  {"uuid": "0194881f-9444-7a73-a458-0af81c034d87", "verdi": false},
                  {"uuid": "0194881f-9444-7a73-a458-0af81c034d88", "verdi": false},
                  {"uuid": "01948d3c-4bea-7802-9d18-5342a5e2be99", "verdi": false},
                  {"uuid": "0194ff86-a035-7eb0-9c60-21899f7cc0c1", "verdi": true},
                  {"uuid": "019f608d-f9c5-7ccc-a6f3-c53531d1f00c", "verdi": false},
                  {"uuid": "01a0c316-a2e9-7088-acb1-da5596bd0055", "verdi": false},
                  {"uuid": "0194881f-9433-70e9-a85b-c246150c45cd", "verdi": false},
                  {"uuid": "0194881f-9433-70e9-a85b-c246150c45ce", "verdi": false},
                  {"uuid": "0194881f-9433-70e9-a85b-c246150c45cf", "verdi": false},
                  {"uuid": "0194881f-9433-70e9-a85b-c246150c45d0", "verdi": false},
                  {"uuid": "0194881f-9433-70e9-a85b-c246150c45d1", "verdi": false},
                  {"uuid": "0194881f-9433-70e9-a85b-c246150c45d2", "verdi": false},
                  {"uuid": "0194881f-9433-70e9-a85b-c246150c45d3", "verdi": false},
                  {"uuid": "0194881f-9435-72a8-b1ce-9575cbc2a75e", "verdi": false},
                  {"uuid": "0194881f-9435-72a8-b1ce-9575cbc2a75f", "verdi": 37.5},
                  {"uuid": "0194881f-9441-7d1b-a06a-6727543a141e", "verdi": true},
                  {"uuid": "0194881f-9442-707b-a6ee-e96c06877bd9", "verdi": true},
                  {"uuid": "0194881f-9442-707b-a6ee-e96c06877bdc", "verdi": true},
                  {"uuid": "0194881f-9442-707b-a6ee-e96c06877bde", "verdi": true},
                  {"uuid": "0194881f-9445-734c-a7ee-045edf29b522", "verdi": false},
                  {"uuid": "0194881f-9447-7e36-a569-3e9f42bff9f6", "verdi": false},
                  {"uuid": "0196ab10-0cff-7301-99d6-65be50a50201", "verdi": true},
                  {"uuid": "0194881f-9428-74d5-b160-f63a4c61a23b", "verdi": {"barn": []}},
                  {"uuid": "0194881f-942e-7cb0-aa59-05ea449d88e0", "verdi": false},
                  {"uuid": "019f26ec-f7e4-791a-af42-6cb632466cfa", "verdi": false}
                ]
                """.trimIndent(),
            ).values()
            .associateBy { it["uuid"].asString() }
}
