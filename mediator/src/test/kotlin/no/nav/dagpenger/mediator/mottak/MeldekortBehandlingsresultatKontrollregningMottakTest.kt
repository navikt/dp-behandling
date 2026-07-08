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
                endretRettighetsperiode = true,
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
        message["detaljer"]["meldekortSendtForSent"].asBoolean() shouldBe true
        message["detaljer"]["harMeldtAnnenAktivitet"].asBoolean() shouldBe false
        message["detaljer"]["harMeldtArbeidstimer"].asBoolean() shouldBe false
        message["detaljer"]["harEndringISats"].asBoolean() shouldBe false
        message["detaljer"]["harEndringiArbeidstid"].asBoolean() shouldBe false
        message["detaljer"]["harEndringITerskel"].asBoolean() shouldBe false
        message["detaljer"]["ileggesSanksjon"].asBoolean() shouldBe false
        message["detaljer"]["harEndretRettighetsperiode"].asBoolean() shouldBe true
    }

    @Test
    fun `publiserer kontrollregningbehov ved for sen melding og sanksjon`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                endretRettighetsperiode = false,
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
        message["detaljer"]["meldekortSendtForSent"].asBoolean() shouldBe true
        message["detaljer"]["harMeldtAnnenAktivitet"].asBoolean() shouldBe false
        message["detaljer"]["harMeldtArbeidstimer"].asBoolean() shouldBe false
        message["detaljer"]["harEndringISats"].asBoolean() shouldBe false
        message["detaljer"]["harEndringiArbeidstid"].asBoolean() shouldBe false
        message["detaljer"]["harEndringITerskel"].asBoolean() shouldBe false
        message["detaljer"]["ileggesSanksjon"].asBoolean() shouldBe true
        message["detaljer"]["harEndretRettighetsperiode"].asBoolean() shouldBe false
    }

    @Test
    fun `publiserer ikke kontrollregningbehov ved arbeidsdag false og ny opplysning utenfor beregning`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                endretRettighetsperiode = false,
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
                endretRettighetsperiode = false,
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
        val message = rapid.inspektør.message(0)
        message["detaljer"]["meldekortSendtForSent"].asBoolean() shouldBe false
        message["detaljer"]["harMeldtAnnenAktivitet"].asBoolean() shouldBe false
        message["detaljer"]["harMeldtArbeidstimer"].asBoolean() shouldBe true
        message["detaljer"]["harEndringISats"].asBoolean() shouldBe false
        message["detaljer"]["harEndringiArbeidstid"].asBoolean() shouldBe true
        message["detaljer"]["harEndringITerskel"].asBoolean() shouldBe false
        message["detaljer"]["ileggesSanksjon"].asBoolean() shouldBe false
        message["detaljer"]["harEndretRettighetsperiode"].asBoolean() shouldBe false
    }

    @Test
    fun `publiserer ikke kontrollregningbehov når meldekort har innhold men ingen endring og ingen stans`() {
        rapid.sendTestMessage(
            behandlingsresultat(
                endretRettighetsperiode = false,
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
                endretRettighetsperiode = true,
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
        endretRettighetsperiode: Boolean,
        opplysninger: List<String>,
    ): String {
        val opprinnelse = if (endretRettighetsperiode) "Ny" else "Arvet"
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
