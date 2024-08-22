package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.Opplysningstype

object Beregning {
    val arbeidsdag = Opplysningstype.somBoolsk("Arbeidsdag")
    val arbeidstimer = Opplysningstype.somHeltall("Arbeidstimer på en arbeidsdag")
    val forbruk = Opplysningstype.somBoolsk("Dag som fører til forbruk av dagpengeperiode")

    // TODO: Er dette noe annet enn krav til tap?
    val terskel = Opplysningstype.somDesimaltall("Terskel for hvor mye arbeid som kan utføres samtidig med dagpenger")
}
