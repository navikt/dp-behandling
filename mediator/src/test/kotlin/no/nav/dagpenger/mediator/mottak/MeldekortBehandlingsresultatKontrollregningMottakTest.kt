package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidstimerId
import no.nav.dagpenger.regel.OpplysningsTyper.maksimalVanligArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.trekkVedForsenMeldingId
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode.harSanksjon
import org.junit.jupiter.api.Test
import java.util.UUID

class MeldekortBehandlingsresultatKontrollregningMottakTest {
    private val rapid = TestRapid()

    init {
        MeldekortBehandlingsresultatKontrollregningMottak(rapid)
    }

    @Test
    fun `publiserer kontrollregningbehov ved for sen melding og stans`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                endretPeriode = true,
                opplysninger =
                    listOf(
                        opplysning(
                            opplysningTypeId = trekkVedForsenMeldingId.uuid,
                            opprinnelse = "NY",
                            verdi = "false",
                        ),
                    ),
            ),
        )

        rapid.inspektør.size shouldBeExactly 1
        val message = rapid.inspektør.message(0)
        message["@event_name"].asString() shouldBe "meldekortberegning_trenger_kontrollregning"
        message["behandlingId"].asString() shouldBe "12345678-1234-1234-1234-123456789012"
        message["detaljer"]["trekkVedForsenMelding"].asBoolean() shouldBe true
        message["detaljer"]["avgjorelseStans"].asBoolean() shouldBe true
        message["detaljer"].has("meldekortMedInnhold") shouldBe false
        message["detaljer"].has("harEndring") shouldBe false
        message["detaljer"]["nyOpplysningUtenforBeregning"].asBoolean() shouldBe false
        message["detaljer"]["ileggesSanksjon"].asBoolean() shouldBe false
    }

    @Test
    fun `publiserer kontrollregningbehov ved for sen melding og sanksjon`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                endretPeriode = true,
                opplysninger =
                    listOf(
                        opplysning(
                            opplysningTypeId = trekkVedForsenMeldingId.uuid,
                            opprinnelse = "NY",
                            verdi = "false",
                        ),
                        opplysning(
                            opplysningTypeId = harSanksjon.id.uuid,
                            opprinnelse = "NY",
                            verdi = "true",
                        ),
                    ),
            ),
        )

        rapid.inspektør.size shouldBeExactly 1
        val message = rapid.inspektør.message(0)
        message["@event_name"].asString() shouldBe "meldekortberegning_trenger_kontrollregning"
        message["behandlingId"].asString() shouldBe "12345678-1234-1234-1234-123456789012"
        message["detaljer"]["trekkVedForsenMelding"].asBoolean() shouldBe true
        message["detaljer"]["avgjorelseStans"].asBoolean() shouldBe true
        message["detaljer"].has("meldekortMedInnhold") shouldBe false
        message["detaljer"].has("harEndring") shouldBe false
        message["detaljer"]["nyOpplysningUtenforBeregning"].asBoolean() shouldBe false
        message["detaljer"]["ileggesSanksjon"].asBoolean() shouldBe true
    }

    @Test
    fun `publiserer ikke kontrollregningbehov ved arbeidsdag false og ny opplysning utenfor beregning`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                endretPeriode = false,
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

        rapid.inspektør.size shouldBeExactly 0
    }

    @Test
    fun `publiserer kontrollregningbehov når meldekort har innhold og endring uten stans`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                endretPeriode = false,
                opplysninger =
                    listOf(
                        opplysning(
                            opplysningTypeId = arbeidstimerId.uuid,
                            opprinnelse = "NY",
                            verdi = "7.5",
                        ),
                        opplysning(
                            opplysningTypeId = fastsattVanligArbeidstid.id.uuid,
                            opprinnelse = "NY",
                            verdi = "0",
                        ),
                    ),
            ),
        )

        rapid.inspektør.size shouldBeExactly 1
    }

    @Test
    fun `publiserer ikke kontrollregningbehov når meldekort har innhold men ingen endring og ingen stans`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                endretPeriode = false,
                opplysninger =
                    listOf(
                        opplysning(
                            opplysningTypeId = maksimalVanligArbeidstidId.uuid,
                            opprinnelse = "NY",
                            verdi = "0",
                        ),
                    ),
            ),
        )

        rapid.inspektør.size shouldBeExactly 0
    }

    @Test
    fun `publiserer ikke kontrollregningbehov for ikke meldekort`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                hendelseType = "Søknad",
                endretPeriode = true,
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
        endretPeriode: Boolean,
        opplysninger: List<String>,
    ): String {
        val opprinnelse = if (endretPeriode) "Ny" else "Arvet"
        val opplysninger = opplysninger.joinToString(",")
        return """
            {
              "@event_name": "behandlingsresultat",
              "ident": "12345678901",
              "behandlingId": "12345678-1234-1234-1234-123456789012",
              "behandletHendelse": {
                "type": "$hendelseType",
                "id": "meldekort-1"
              },
              "rettighetsperioder": [{"opprinnelse": "$opprinnelse"}],
              "opplysninger": [$opplysninger]
            }
            """.trimIndent()
    }

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
