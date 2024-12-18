package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnbeløpForDagpengeGrunnlag

object VernepliktFastsetting {
    private val faktor = Opplysningstype.somHeltall("Faktor")
    internal val vernepliktGrunnlag = Opplysningstype.somBeløp("Grunnlag for verneplikt")
    internal val vernepliktPeriode = Opplysningstype.somHeltall("Vernepliktperiode")

    val regelsett =
        Regelsett("VernepliktFastsetting") {
            regel(faktor) { oppslag(prøvingsdato) { 3 } }
            regel(vernepliktGrunnlag) { multiplikasjon(grunnbeløpForDagpengeGrunnlag, faktor) }
            regel(vernepliktPeriode) { oppslag(prøvingsdato) { 26 } }
        }

    val ønsketResultat = listOf(vernepliktGrunnlag, vernepliktPeriode)
}
