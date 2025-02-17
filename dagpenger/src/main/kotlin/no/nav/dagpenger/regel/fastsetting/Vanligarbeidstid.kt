package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.regel.OpplysningsTyper.fastsattArbeidstidPerUkeFørTapId
import no.nav.dagpenger.regel.ReellArbeidssøker.ønsketArbeidstid
import no.nav.dagpenger.regel.Samordning.samordnetBeregnetArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.maksimalVanligArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.ordinærEllerVernepliktArbeidstid
import no.nav.dagpenger.regel.folketrygden
import no.nav.dagpenger.regel.kravPåDagpenger
import no.nav.dagpenger.regel.oppfyllerKravetTilMinsteinntektEllerVerneplikt

object Vanligarbeidstid {
    val fastsattVanligArbeidstid = Opplysningstype.desimaltall(fastsattArbeidstidPerUkeFørTapId, "Fastsatt arbeidstid per uke før tap")
    val regelsett =
        fastsettelse(
            folketrygden.hjemmel(4, 3, "Fastsettelse av arbeidstid", "Fastsettelse av arbeidstid"),
        ) {
            skalVurderes { kravPåDagpenger(it) }

            regel(fastsattVanligArbeidstid) {
                minstAv(
                    maksimalVanligArbeidstid,
                    ordinærEllerVernepliktArbeidstid,
                    samordnetBeregnetArbeidstid,
                    ønsketArbeidstid,
                )
            }
            påvirkerResultat {
                oppfyllerKravetTilMinsteinntektEllerVerneplikt(it)
            }
        }
}
