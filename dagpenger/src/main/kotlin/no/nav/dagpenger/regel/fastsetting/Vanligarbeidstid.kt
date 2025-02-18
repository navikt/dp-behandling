package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.høyesteAv
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.OpplysningsTyper.fastsattArbeidstidPerUkeFørTapId
import no.nav.dagpenger.regel.OpplysningsTyper.minsteTillatteArbeidstidId
import no.nav.dagpenger.regel.OpplysningsTyper.minstØnsketArbeidstidId
import no.nav.dagpenger.regel.ReellArbeidssøker.ønsketArbeidstid
import no.nav.dagpenger.regel.Samordning.samordnetBeregnetArbeidstid
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.maksimalVanligArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.ordinærEllerVernepliktArbeidstid
import no.nav.dagpenger.regel.folketrygden
import no.nav.dagpenger.regel.kravPåDagpenger
import no.nav.dagpenger.regel.oppfyllerKravetTilMinsteinntektEllerVerneplikt

object Vanligarbeidstid {
    val fastsattVanligArbeidstid = Opplysningstype.desimaltall(fastsattArbeidstidPerUkeFørTapId, "Fastsatt arbeidstid per uke før tap")

    private val minsteTillatteArbeidstid =
        Opplysningstype.desimaltall(minsteTillatteArbeidstidId, "Minimum fastsatt arbeidstid per uke", synlig = { false })
    private val minstØnsketArbeidstid =
        Opplysningstype.desimaltall(
            minstØnsketArbeidstidId,
            "Minste ønsket arbeidstid som kan inngå fastsettelsen av arbeidstid",
            synlig = { false },
        )
    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 3, "Fastsettelse av arbeidstid", "Fastsettelse av arbeidstid"),
        ) {
            skalVurderes { kravPåDagpenger(it) }

            regel(minsteTillatteArbeidstid) { oppslag(prøvingsdato) { 0.5 } }
            regel(minstØnsketArbeidstid) { høyesteAv(ønsketArbeidstid, minsteTillatteArbeidstid) }
            regel(fastsattVanligArbeidstid) {
                minstAv(
                    maksimalVanligArbeidstid,
                    ordinærEllerVernepliktArbeidstid,
                    samordnetBeregnetArbeidstid,
                    minstØnsketArbeidstid,
                )
            }
            påvirkerResultat {
                oppfyllerKravetTilMinsteinntektEllerVerneplikt(it)
            }
        }
}
