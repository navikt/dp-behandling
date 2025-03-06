package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidstimerId
import no.nav.dagpenger.regel.OpplysningsTyper.forbrukId
import no.nav.dagpenger.regel.OpplysningsTyper.meldeperiodeId
import no.nav.dagpenger.regel.OpplysningsTyper.meldtId
import no.nav.dagpenger.regel.OpplysningsTyper.terskelId

object Beregning {
    val meldeperiode = Opplysningstype.periode(meldeperiodeId, "Meldeperiode")
    val arbeidsdag = Opplysningstype.boolsk(arbeidsdagId, "Arbeidsdag")
    val arbeidstimer = Opplysningstype.heltall(arbeidstimerId, "Arbeidstimer på en arbeidsdag")
    val forbruk = Opplysningstype.boolsk(forbrukId, "Dag som fører til forbruk av dagpengeperiode")
    val meldt = Opplysningstype.boolsk(meldtId, "Har meldt seg via meldekort")

    // TODO: Er dette noe annet enn krav til tap?
    val terskel = Opplysningstype.desimaltall(terskelId, "Terskel for hvor mye arbeid som kan utføres samtidig med dagpenger")

    val regelsett =
        fastsettelse(tomHjemmel("regelsett")) {
            // Finn alle meldeperioder som ikke er beregnet
            // Er det noe å beregne?
            // 1. Har dagpenger i perioden?

            // Er det trekk ved for sen melding?

            // Beregne noe greier
        }
}
