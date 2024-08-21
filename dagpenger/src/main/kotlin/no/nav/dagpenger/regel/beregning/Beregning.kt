package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.Opplysningstype

object Beregning {
    val arbeidsdag = Opplysningstype.somHeltall("Arbeidsdager")

    // TODO: Er dette noe annet enn krav til tap?
    val terskel = Opplysningstype.somDesimaltall("Terskel")
}
