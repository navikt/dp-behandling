package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.tomRegel
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidstimerId
import no.nav.dagpenger.regel.OpplysningsTyper.forbrukId
import no.nav.dagpenger.regel.OpplysningsTyper.forbruktEgenandelId
import no.nav.dagpenger.regel.OpplysningsTyper.forbrukteDagerId
import no.nav.dagpenger.regel.OpplysningsTyper.gjenståendeDagerId
import no.nav.dagpenger.regel.OpplysningsTyper.meldeperiodeId
import no.nav.dagpenger.regel.OpplysningsTyper.meldtId
import no.nav.dagpenger.regel.OpplysningsTyper.terskelId
import no.nav.dagpenger.regel.OpplysningsTyper.utbetalingId

object Beregning {
    val meldeperiode = Opplysningstype.periode(meldeperiodeId, "Meldeperiode", synlig = aldriSynlig)
    val arbeidsdag = Opplysningstype.boolsk(arbeidsdagId, "Arbeidsdag")
    val arbeidstimer = Opplysningstype.desimaltall(arbeidstimerId, "Arbeidstimer på en arbeidsdag")

    val forbruk = Opplysningstype.boolsk(forbrukId, "Dag som fører til forbruk av dagpengeperiode")
    val meldt = Opplysningstype.boolsk(meldtId, "Har meldt seg via meldekort")

    val forbruktEgenandel = Opplysningstype.heltall(forbruktEgenandelId, "Forbrukt egenandel")
    val utbetaling = Opplysningstype.heltall(utbetalingId, "Penger som skal utbetales")

    val forbrukt = Opplysningstype.heltall(forbrukteDagerId, "Antall dager som er forbrukt")
    val gjenstående = Opplysningstype.heltall(gjenståendeDagerId, "Antall dager som gjenstår")

    // TODO: Er dette noe annet enn krav til tap?
    val terskel = Opplysningstype.desimaltall(terskelId, "Terskel for hvor mye arbeid som kan utføres samtidig med dagpenger")

    val regelsett =
        fastsettelse(tomHjemmel("Meldekortberegning")) {
            skalVurderes { it.har(meldeperiode) }
            påvirkerResultat { it.har(meldeperiode) }

            regel(meldeperiode) { tomRegel }
            regel(arbeidsdag) { tomRegel }
            regel(arbeidstimer) { tomRegel }
            regel(forbruk) { tomRegel }
            regel(meldt) { tomRegel }
            regel(forbruktEgenandel) { tomRegel }
            regel(utbetaling) { tomRegel }
            regel(terskel) { tomRegel }

            regel(forbrukt) { tomRegel }
            regel(gjenstående) { tomRegel }

            ønsketResultat(
                meldeperiode,
                arbeidsdag,
                arbeidstimer,
                forbruk,
                meldt,
                forbruktEgenandel,
                utbetaling,
                terskel,
                forbrukt,
                gjenstående,
            )
        }
}
