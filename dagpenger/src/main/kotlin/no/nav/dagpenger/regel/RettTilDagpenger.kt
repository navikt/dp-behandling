package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.alle

object RettTilDagpenger {
    val kravPåDagpenger = Opplysningstype.somBoolsk("Krav på dagpenger")
    val regelsett =
        Regelsett("Krav på dagpenger") {
            regel(kravPåDagpenger) { alle(Alderskrav.vilkår, Minsteinntekt.minsteinntekt) }
        }
}
