package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.hendelse.MigrerYtelseHendelse
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MigrerYtelseMottakTest {
    private val ekstra = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "ekstra")
    private val rapid = TestRapid()
    private val hendelseMottaker = mockk<HendelseMottaker>(relaxed = true)

    init {
        MigrerYtelseMottak(rapid, hendelseMottaker, setOf(ekstra, DagpengenesStørrelse.antallBarn))
    }

    @Test
    fun `oppretter behandling med kontraktsfelter og tilleggsopplysninger`() {
        rapid.sendTestMessage(melding(opplysninger = """{ "${ekstra.behovId}": true }"""))

        val hendelse = slot<MigrerYtelseHendelse>()
        verify(exactly = 1) { hendelseMottaker.behandle(capture(hendelse), any(), any()) }

        val behandling =
            hendelse.captured
                .behandling(null, TemporalCollection())
                .shouldBeInstanceOf<StartHendelseResultat.Opprettet>()
                .behandling
        val opplysninger = behandling.opplysninger()

        opplysninger.finnOpplysning(ekstra).verdi shouldBe true
        opplysninger.finnOpplysning(DagpengenesStørrelse.antallBarn).verdi shouldBe 2
        opplysninger.finnOpplysning(Dagpengeperiode.antallStønadsdager).verdi shouldBe 120
        opplysninger.finnOpplysning(DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg).verdi shouldBe Beløp(800)
    }

    @Test
    fun `kan ikke oppgi samme opplysning både eksplisitt og som tilleggsopplysning`() {
        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(melding(opplysninger = """{ "${DagpengenesStørrelse.antallBarn.behovId}": 3 }"""))
        }
    }

    @Test
    fun `avviser tilleggsopplysning med feil verdityper`() {
        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(melding(opplysninger = """{ "${ekstra.behovId}": "ja" }"""))
        }
    }

    @Test
    fun `avviser ukjent opplysningstype`() {
        shouldThrow<IllegalArgumentException> {
            rapid.sendTestMessage(melding(opplysninger = """{ "finnes ikke": true }"""))
        }
    }

    private fun melding(opplysninger: String) =
        """
        {
          "@event_name": "migrer_ytelse",
          "ident": "12345678910",
          "innvilgetFraOgMed": "${LocalDate.of(2026, 1, 5)}",
          "innvilgedeStønadsuker": 52,
          "gjenståendeStønadsdager": 120,
          "dagsats": "800",
          "antallBarn": 2,
          "vanligArbeidstidPerUke": 37.5,
          "grunnlag": "517349",
          "opplysninger": $opplysninger
        }
        """.trimIndent()
}
