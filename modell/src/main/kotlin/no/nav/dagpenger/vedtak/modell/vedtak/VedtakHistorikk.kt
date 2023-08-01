package no.nav.dagpenger.vedtak.modell.vedtak

import de.fxlae.typeid.TypeId
import no.nav.dagpenger.vedtak.modell.Dagpengerettighet
import no.nav.dagpenger.vedtak.modell.TemporalCollection
import no.nav.dagpenger.vedtak.modell.entitet.Beløp
import no.nav.dagpenger.vedtak.modell.entitet.Stønadsdager
import no.nav.dagpenger.vedtak.modell.entitet.Timer
import no.nav.dagpenger.vedtak.modell.utbetaling.Utbetalingsdag
import no.nav.dagpenger.vedtak.modell.vedtak.Vedtak.Companion.harBehandlet
import no.nav.dagpenger.vedtak.modell.vedtak.rettighet.Ordinær
import no.nav.dagpenger.vedtak.modell.vedtak.rettighet.Permittering
import no.nav.dagpenger.vedtak.modell.vedtak.rettighet.PermitteringFraFiskeindustrien
import no.nav.dagpenger.vedtak.modell.visitor.VedtakHistorikkVisitor
import no.nav.dagpenger.vedtak.modell.visitor.VedtakVisitor
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakHistorikk internal constructor(private val vedtak: MutableList<Vedtak>) {

    internal constructor() : this(mutableListOf<Vedtak>())

    private val observers = mutableSetOf<VedtakObserver>()

    internal val vanligArbeidstidHistorikk = TemporalCollection<Timer>()
    internal val dagsatsHistorikk = TemporalCollection<Beløp>()
    internal val dagpengerettighetHistorikk = TemporalCollection<Dagpengerettighet>()

    internal val stønadsdagerHistorikk = TemporalCollection<Stønadsdager>()
    internal val forbrukHistorikk = ForbrukHistorikk()
    internal val egenandelHistorikk = TemporalCollection<Beløp>()
    internal val trukketEgenandelHistorikk = TrukketEgenandelHistorikk()
    private val beløpTilUtbetalingHistorikk = TemporalCollection<Beløp>()

    init {
        vedtak.forEach { HistorikkOppdaterer(this).apply(it::accept) }
    }
    fun addObserver(vedtakObserver: VedtakObserver) {
        this.observers.add(vedtakObserver)
    }

    fun accept(visitor: VedtakHistorikkVisitor) {
        visitor.preVisitVedtak()
        vedtak.forEach { it.accept(visitor) }
        visitor.postVisitVedtak()
    }

    fun gjenståendeStønadsdagerFra(dato: LocalDate): Stønadsdager =
        stønadsdagerHistorikk.get(dato) - forbrukHistorikk.summer(dato)

    fun gjenståendeEgenandelFra(dato: LocalDate): Beløp =
        egenandelHistorikk.get(dato) - trukketEgenandelHistorikk.summer(dato)

    fun beløpTilUtbetalingFor(dato: LocalDate): Beløp = beløpTilUtbetalingHistorikk.get(dato)

    internal fun leggTilVedtak(vedtak: Vedtak) {
        this.vedtak.add(
            vedtak.also {
                HistorikkOppdaterer(this).apply(it::accept)
            },
        )
        this.observers.forEach { vedtakObserver ->
            when (vedtak) {
                is Utbetalingsvedtak -> {
                    val utbetalingVedtakFattet = VedtakFattetVisitor().apply(vedtak::accept).utbetalingsvedtakFattet
                    vedtakObserver.utbetalingsvedtakFattet(utbetalingVedtakFattet)
                }

                else -> vedtakObserver.vedtakFattet(VedtakFattetVisitor().apply(vedtak::accept).vedtakFattet)
            }
        }
    }

    internal fun harBehandlet(behandlingId: TypeId) = this.vedtak.harBehandlet(behandlingId)

    private class HistorikkOppdaterer(private val vedtakHistorikk: VedtakHistorikk) : VedtakVisitor {

        private var virkningsdato: LocalDate? = null

        private fun virkningsdato() = requireNotNull(virkningsdato) { " Forventet at virkninsdato er satt. Har du husket preVisitVedtak?" }
        override fun preVisitVedtak(
            vedtakId: TypeId,
            behandlingId: TypeId,
            virkningsdato: LocalDate,
            vedtakstidspunkt: LocalDateTime,
            type: Vedtak.VedtakType,
        ) {
            this.virkningsdato = virkningsdato
        }

        override fun visitOrdinær(ordinær: Ordinær) {
            vedtakHistorikk.dagpengerettighetHistorikk.put(virkningsdato(), Dagpengerettighet.Ordinær)
        }

        override fun visitPermitteringFraFiskeindustrien(permitteringFraFiskeindustrien: PermitteringFraFiskeindustrien) {
            vedtakHistorikk.dagpengerettighetHistorikk.put(virkningsdato(), Dagpengerettighet.PermitteringFraFiskeindustrien)
        }

        override fun visitPermittering(permittering: Permittering) {
            vedtakHistorikk.dagpengerettighetHistorikk.put(virkningsdato(), Dagpengerettighet.Permittering)
        }

        override fun visitAntallStønadsdager(dager: Stønadsdager) {
            vedtakHistorikk.stønadsdagerHistorikk.put(virkningsdato(), dager)
        }

        override fun visitVanligArbeidstidPerDag(timer: Timer) {
            vedtakHistorikk.vanligArbeidstidHistorikk.put(virkningsdato(), timer)
        }

        override fun visitDagsats(beløp: Beløp) {
            vedtakHistorikk.dagsatsHistorikk.put(virkningsdato(), beløp)
        }

        override fun visitEgenandel(beløp: Beløp) {
            vedtakHistorikk.egenandelHistorikk.put(virkningsdato(), beløp)
        }

        override fun postVisitVedtak(
            vedtakId: TypeId,
            behandlingId: TypeId,
            virkningsdato: LocalDate,
            vedtakstidspunkt: LocalDateTime,
            type: Vedtak.VedtakType,
        ) {
            this.virkningsdato = null
        }

        override fun visitUtbetalingsvedtak(
            utfall: Boolean,
            forbruk: Stønadsdager,
            trukketEgenandel: Beløp,
            beløpTilUtbetaling: Beløp,
            utbetalingsdager: List<Utbetalingsdag>,
        ) {
            vedtakHistorikk.forbrukHistorikk.put(this.virkningsdato(), forbruk)
            vedtakHistorikk.trukketEgenandelHistorikk.put(this.virkningsdato(), trukketEgenandel)
            vedtakHistorikk.beløpTilUtbetalingHistorikk.put(this.virkningsdato(), beløpTilUtbetaling)
        }

        override fun visitAvslag(
            vedtakId: TypeId,
            behandlingId: TypeId,
            vedtakstidspunkt: LocalDateTime,
            utfall: Boolean,
            virkningsdato: LocalDate,
        ) {
        }

        override fun visitStans(
            vedtakId: TypeId,
            behandlingId: TypeId,
            virkningsdato: LocalDate,
            vedtakstidspunkt: LocalDateTime,
            utfall: Boolean?,
        ) {
            vedtakHistorikk.dagpengerettighetHistorikk.put(virkningsdato, Dagpengerettighet.Ingen)
        }
    }
}
