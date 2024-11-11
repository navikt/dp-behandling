package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.tekstId
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato

object Egenandel {
    val egenandel = Opplysningstype.somBeløp("Egenandel".tekstId("opplysning.egenandel"))
    private val sats = DagpengenesStørrelse.avrundetDagsMedBarnetillegg
    private val faktor = Opplysningstype.somDesimaltall("Antall dagsats for egenandel")

    val regelsett =
        Regelsett("§ 4-9. Egenandel") {
            regel(faktor) { oppslag(prøvingsdato) { 3.0 } }
            regel(egenandel) { multiplikasjon(sats, faktor) }
        }

    val ønsketResultat = listOf(egenandel)
}
