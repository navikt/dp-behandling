package no.nav.dagpenger.vedtak.modell.visitor

import de.fxlae.typeid.TypeId
import no.nav.dagpenger.vedtak.modell.entitet.Beløp
import no.nav.dagpenger.vedtak.modell.entitet.Stønadsdager
import no.nav.dagpenger.vedtak.modell.utbetaling.Utbetalingsdag
import no.nav.dagpenger.vedtak.modell.vedtak.Vedtak
import java.time.LocalDate
import java.time.LocalDateTime

interface VedtakVisitor : FaktumVisitor, RettighetVisitor {

    fun preVisitVedtak(
        vedtakId: TypeId,
        behandlingId: TypeId,
        virkningsdato: LocalDate,
        vedtakstidspunkt: LocalDateTime,
        type: Vedtak.VedtakType,
    ) {}

    fun postVisitVedtak(
        vedtakId: TypeId,
        behandlingId: TypeId,
        virkningsdato: LocalDate,
        vedtakstidspunkt: LocalDateTime,
        type: Vedtak.VedtakType,
    ) {}

    fun visitUtbetalingsvedtak(
        utfall: Boolean,
        forbruk: Stønadsdager,
        trukketEgenandel: Beløp,
        beløpTilUtbetaling: Beløp,
        utbetalingsdager: List<Utbetalingsdag>,
    ) {}

    fun visitAvslag(
        vedtakId: TypeId,
        behandlingId: TypeId,
        vedtakstidspunkt: LocalDateTime,
        utfall: Boolean,
        virkningsdato: LocalDate,
    ) {}

    fun visitStans(
        vedtakId: TypeId,
        behandlingId: TypeId,
        virkningsdato: LocalDate,
        vedtakstidspunkt: LocalDateTime,
        utfall: Boolean?,
    ) {}
}
