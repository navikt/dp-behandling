package no.nav.dagpenger.vedtak.modell.vedtak

import de.fxlae.typeid.TypeId
import no.nav.dagpenger.vedtak.modell.visitor.VedtakVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class Stansvedtak(
    vedtakId: TypeId = TypeId.generate(idPrefix),
    behandlingId: TypeId,
    vedtakstidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
    virkningsdato: LocalDate,
    private val utfall: Boolean = false,
) : Vedtak(vedtakId, behandlingId, vedtakstidspunkt, virkningsdato, VedtakType.Stans) {
    override fun accept(visitor: VedtakVisitor) {
        visitor.visitStans(
            vedtakId,
            behandlingId,
            virkningsdato,
            vedtakstidspunkt,
            utfall,
        )
    }
}
