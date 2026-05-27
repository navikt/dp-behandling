package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidstimerId
import no.nav.dagpenger.regel.OpplysningsTyper.trekkVedForsenMeldingId
import org.junit.jupiter.api.Test
import java.util.UUID

class MeldekortBehandlingsresultatSignalMottakTest {
    private val rapid = TestRapid()

    init {
        MeldekortBehandlingsresultatSignalMottak(rapid)
    }

    @Test
    fun `publiserer signal ved for sen melding og stans`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                førteTil = "STANS",
                opplysninger =
                    listOf(
                        opplysning(
                            opplysningTypeId = trekkVedForsenMeldingId.uuid,
                            opprinnelse = "NY",
                            verdi = "true",
                        ),
                    ),
            ),
        )

        rapid.inspektør.size shouldBeExactly 1
        with(rapid.inspektør.message(0)) {
            this["@event_name"].asString() shouldBe "meldekort_behandlingsresultat_signal"
            this["behandlingId"].asString() shouldBe "12345678-1234-1234-1234-123456789012"
            this["trigger"]["trekkVedForsenMelding"].asBoolean() shouldBe true
            this["trigger"]["avgjorelseStans"].asBoolean() shouldBe true
        }
    }

    @Test
    fun `publiserer signal ved arbeidsdag false og ny opplysning utenfor beregning`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                førteTil = "ENDRING",
                opplysninger =
                    listOf(
                        opplysning(
                            opplysningTypeId = arbeidsdagId.uuid,
                            opprinnelse = "NY",
                            verdi = "false",
                        ),
                        opplysning(
                            opplysningTypeId = UUID.randomUUID(),
                            opprinnelse = "NY",
                            verdi = "1",
                        ),
                    ),
            ),
        )

        rapid.inspektør.size shouldBeExactly 1
        with(rapid.inspektør.message(0)) {
            this["@event_name"].asString() shouldBe "meldekort_behandlingsresultat_signal"
            this["trigger"]["arbeidsdagUtenArbeid"].asBoolean() shouldBe true
            this["trigger"]["nyOpplysningUtenforBeregning"].asBoolean() shouldBe true
        }
    }

    @Test
    fun `publiserer ikke signal når stoppsignal mangler`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                førteTil = "ENDRING",
                opplysninger =
                    listOf(
                        opplysning(
                            opplysningTypeId = arbeidstimerId.uuid,
                            opprinnelse = "NY",
                            verdi = "7.5",
                        ),
                    ),
            ),
        )

        rapid.inspektør.size shouldBeExactly 0
    }

    @Test
    fun `publiserer ikke signal for ikke meldekort`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                hendelseType = "Søknad",
                førteTil = "STANS",
                opplysninger =
                    listOf(
                        opplysning(
                            opplysningTypeId = trekkVedForsenMeldingId.uuid,
                            opprinnelse = "NY",
                            verdi = "true",
                        ),
                    ),
            ),
        )

        rapid.inspektør.size shouldBeExactly 0
    }

    private fun behandlingsresultat(
        hendelseType: String = "Meldekort",
        førteTil: String,
        opplysninger: List<String>,
    ) = """
        {
          "@event_name": "behandlingsresultat",
          "ident": "12345678901",
          "behandlingId": "12345678-1234-1234-1234-123456789012",
          "behandletHendelse": {
            "type": "$hendelseType",
            "id": "meldekort-1"
          },
          "førteTil": "$førteTil",
          "opplysninger": [${opplysninger.joinToString(",")}]
        }
        """.trimIndent()

    private fun opplysning(
        opplysningTypeId: UUID,
        opprinnelse: String,
        verdi: String,
    ) = """
        {
          "opplysningTypeId": "$opplysningTypeId",
          "perioder": [
            {
              "opprinnelse": "$opprinnelse",
              "verdi": {
                "verdi": $verdi
              }
            }
          ]
        }
        """.trimIndent()
}
