package no.nav.dagpenger.vedtak.modell.rapportering

import de.fxlae.typeid.TypeId
import no.nav.dagpenger.vedtak.modell.entitet.Periode
import no.nav.dagpenger.vedtak.modell.visitor.RapporteringsperiodeVisitor
import java.time.LocalDate
import java.util.SortedSet

class Rapporteringsperiode(private val rapporteringsId: TypeId, private val periode: Periode, dager: List<Dag>) : ClosedRange<LocalDate> by periode {
    constructor(rapporteringsId: TypeId, periode: Periode) : this(rapporteringsId, periode, emptyList())

    private val dager: SortedSet<Dag> = dager.toSortedSet()

    fun leggTilDag(dag: Dag) {
        dager.add(dag)
    }

    fun accept(visitor: RapporteringsperiodeVisitor) {
        visitor.preVisitRapporteringsperiode(rapporteringsId, this)
        dager.forEach { it.accept(visitor) }
        visitor.postVisitRapporteringsperiode(rapporteringsId, this)
    }

    companion object {

        val idPrefix = "rapportering"
        internal fun Iterable<Rapporteringsperiode>.merge(other: Rapporteringsperiode): List<Rapporteringsperiode> {
            val index = this.indexOfFirst { it.sammenfallerMed(other) }
            if (index == -1) return this.toMutableList().also { it.add(other) }
            return this.mapIndexed { i, rapporteringsperiode -> if (i == index) other else rapporteringsperiode }
        }
    }

    private fun sammenfallerMed(other: Rapporteringsperiode): Boolean =
        this.dager.first().sammenfallerMed(other.dager.first())
}
