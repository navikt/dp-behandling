package no.nav.dagpenger.vedtak.iverksetting.mediator.mottak

import de.fxlae.typeid.TypeId
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.vedtak.august
import no.nav.dagpenger.vedtak.iverksetting.IverksettingsVedtak.Utfall.Innvilget
import no.nav.dagpenger.vedtak.iverksetting.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.vedtak.iverksetting.mediator.fattetVedtakJsonHendelse
import no.nav.dagpenger.vedtak.iverksetting.mediator.løpendeVedtakFattet
import no.nav.dagpenger.vedtak.juni
import no.nav.dagpenger.vedtak.mediator.IHendelseMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class VedtakFattetMottakTest {

    private val testRapid = TestRapid()
    private val iHendelseMediator = mockk<IHendelseMediator>()
    private val vedtakId = UUID.fromString("df5e6587-a3e3-407c-8202-02f9740a09b0")
    private val behandlingId = UUID.fromString("0AAA66B9-35C2-4398-ACA0-D1D0A9465292")
    private val ident = "12345678910"

    init {
        VedtakFattetMottak(testRapid, iHendelseMediator)
    }

    @Test
    fun `Skal lese vedtakfattet hendelser`() {
        val vedtakFattetHendelseSlot = slot<VedtakFattetHendelse>()
        every { iHendelseMediator.behandle(capture(vedtakFattetHendelseSlot), any(), any()) } just Runs
        val fattetvedtakJson = fattetVedtakJsonHendelse(ident = ident, vedtakId = vedtakId, behandlingId = behandlingId)
        testRapid.sendTestMessage(fattetvedtakJson)

        verify(exactly = 1) {
            iHendelseMediator.behandle(any<VedtakFattetHendelse>(), any(), any())
        }

        assertSoftly {
            vedtakFattetHendelseSlot.isCaptured shouldBe true
            val vedtakFattetHendelse = vedtakFattetHendelseSlot.captured
            vedtakFattetHendelse.ident() shouldBe ident
            vedtakFattetHendelse.iverksettingsVedtak.vedtakId shouldBe vedtakId
            vedtakFattetHendelse.iverksettingsVedtak.behandlingId shouldBe behandlingId
            vedtakFattetHendelse.iverksettingsVedtak.virkningsdato shouldBe (24 august 2019)
            vedtakFattetHendelse.iverksettingsVedtak.vedtakstidspunkt shouldBe LocalDateTime.MAX
            vedtakFattetHendelse.iverksettingsVedtak.utbetalingsdager shouldBe emptyList()
            vedtakFattetHendelse.iverksettingsVedtak.utfall shouldBe Innvilget
        }
    }

    @Test
    fun `Skal lese løpendeVedtakFattet hendelser`() {
        val typeId = TypeId.generate("vedtak")
        typeId shouldBeEqual TypeId.of("vedtak", typeId.uuid)
        typeId.hashCode() shouldBeEqual TypeId.of("vedtak", typeId.uuid).hashCode()

        val løpendeVedtakFattetSlot = slot<VedtakFattetHendelse>()
        every { iHendelseMediator.behandle(capture(løpendeVedtakFattetSlot), any(), any()) } just Runs
        val løpendeVedtakJson = løpendeVedtakFattet(ident = ident, vedtakId = vedtakId, behandlingId = behandlingId)
        testRapid.sendTestMessage(løpendeVedtakJson)

        verify(exactly = 1) {
            iHendelseMediator.behandle(any<VedtakFattetHendelse>(), any(), any())
        }

        assertSoftly {
            løpendeVedtakFattetSlot.isCaptured shouldBe true
            val løpendeVedtakFattet = løpendeVedtakFattetSlot.captured
            løpendeVedtakFattet.ident() shouldBe ident
            løpendeVedtakFattet.iverksettingsVedtak.vedtakId shouldBe vedtakId
            løpendeVedtakFattet.iverksettingsVedtak.behandlingId shouldBe behandlingId
            løpendeVedtakFattet.iverksettingsVedtak.virkningsdato shouldBe (11 juni 2023)
            løpendeVedtakFattet.iverksettingsVedtak.vedtakstidspunkt shouldBe LocalDateTime.MAX
            løpendeVedtakFattet.iverksettingsVedtak.utbetalingsdager.size shouldBe 10
            løpendeVedtakFattet.iverksettingsVedtak.utfall shouldBe Innvilget
        }
    }
}
