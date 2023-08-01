package no.nav.dagpenger.vedtak.modell.vedtak

import de.fxlae.typeid.TypeId
import no.nav.dagpenger.vedtak.modell.visitor.VedtakVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class Avslag private constructor(
    vedtakId: TypeId,
    behandlingId: TypeId,
    vedtakstidspunkt: LocalDateTime,
    virkningsdato: LocalDate,
    private val utfall: Boolean = false,
) : Vedtak(
    vedtakId = vedtakId,
    behandlingId = behandlingId,
    vedtakstidspunkt = vedtakstidspunkt,
    virkningsdato = virkningsdato,
    type = VedtakType.Avslag,
) {

    companion object {
        fun avslag(behandlingId: TypeId, virkningsdato: LocalDate) =
            Avslag(behandlingId = behandlingId, virkningsdato = virkningsdato)
    }
    constructor(behandlingId: TypeId, virkningsdato: LocalDate) : this(
        vedtakId = TypeId.generate(idPrefix),
        behandlingId = behandlingId,
        vedtakstidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        virkningsdato = virkningsdato,
    )

    override fun accept(visitor: VedtakVisitor) {
        visitor.visitAvslag(
            vedtakId = vedtakId,
            behandlingId = behandlingId,
            vedtakstidspunkt = vedtakstidspunkt,
            utfall = utfall,
            virkningsdato = virkningsdato,
        )
    }
}
