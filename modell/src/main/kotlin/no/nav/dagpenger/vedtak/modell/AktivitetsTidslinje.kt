package no.nav.dagpenger.vedtak.modell

import no.nav.dagpenger.vedtak.modell.hendelser.RapporteringHendelse

class AktivitetsTidslinje {

    val rapporteringsPerioder = mutableListOf<RapporteringsPeriode>()

    fun håndter(rapporteringHendelse: RapporteringHendelse) {
        val dager: List<Dag> = rapporteringHendelse.meldekortDager.map { Dag.lagDag(it.dato) }
        val rapporteringsperiode = RapporteringsPeriode(dager)
        rapporteringsPerioder.add(rapporteringsperiode)
    }
}
