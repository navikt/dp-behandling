package no.nav.dagpenger.vedtak.modell.visitor

import de.fxlae.typeid.TypeId
import no.nav.dagpenger.vedtak.modell.rapportering.Rapporteringsperiode

interface RapporteringsperiodeVisitor : DagVisitor {
    fun preVisitRapporteringsperiode(rapporteringsperiodeId: TypeId, periode: Rapporteringsperiode) {}
    fun postVisitRapporteringsperiode(rapporteringsperiodeId: TypeId, periode: Rapporteringsperiode) {}
}
