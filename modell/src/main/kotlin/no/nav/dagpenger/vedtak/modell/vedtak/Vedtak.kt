package no.nav.dagpenger.vedtak.modell.vedtak

import de.fxlae.typeid.TypeId
import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.vedtak.modell.visitor.VedtakVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

abstract class Vedtak(
    protected val vedtakId: TypeId = TypeId.generate(idPrefix),
    protected val behandlingId: TypeId,
    protected val vedtakstidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
    protected val virkningsdato: LocalDate,
    protected val type: VedtakType,
) : Comparable<Vedtak>, Aktivitetskontekst {

    init {
        require(vedtakId.prefix == idPrefix) { "Alle vedtak ID må prefikses '$idPrefix" }
    }

    enum class VedtakType {
        Ramme,
        Utbetaling,
        Avslag,
        Stans,
    }

    companion object {
        val idPrefix = "vedtak"
        internal fun Collection<Vedtak>.harBehandlet(behandlingId: TypeId): Boolean =
            this.any { it.behandlingId == behandlingId }

        private val etterVedtakstidspunkt = Comparator<Vedtak> { a, b -> a.vedtakstidspunkt.compareTo(b.vedtakstidspunkt) }
    }

    abstract fun accept(visitor: VedtakVisitor)

    override fun compareTo(other: Vedtak) = etterVedtakstidspunkt.compare(this, other)

    override fun toSpesifikkKontekst(): SpesifikkKontekst = SpesifikkKontekst(
        kontekstType = this.javaClass.simpleName,
        kontekstMap = mapOf(
            "vedtakId" to vedtakId.toString(),
        ),
    )
}
