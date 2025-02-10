package no.nav.dagpenger.regel.fastsetting

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.regel.OpplysningsTyper.fastsattArbeidstidPerUkeFørTapId
import no.nav.dagpenger.regel.ReellArbeidssøker.ønsketArbeidstid
import no.nav.dagpenger.regel.Samordning.samordnetBeregnetArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.maksimalVanligArbeidstid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.ordinærEllerVernepliktArbeidstid
import no.nav.dagpenger.regel.folketrygden
import no.nav.dagpenger.regel.kravPåDagpenger
import no.nav.dagpenger.regel.kravetTilAlderOgMinsteinntektErOppfylt

object Vanligarbeidstid {
    val fastsattVanligArbeidstid = Opplysningstype.desimaltall(fastsattArbeidstidPerUkeFørTapId, "Fastsatt arbeidstid per uke før tap")
    val regelsett =
        Regelsett(
            folketrygden.hjemmel(4, 3, "Fastsettelse av arbeidstid", "Fastsettelse av arbeidstid"),
            RegelsettType.Fastsettelse,
        ) {
            skalKjøres { kravPåDagpenger(it) }

            regel(fastsattVanligArbeidstid) {
                minstAv(
                    maksimalVanligArbeidstid,
                    ordinærEllerVernepliktArbeidstid,
                    samordnetBeregnetArbeidstid,
                    ønsketArbeidstid,
                )
            }
            relevantHvis {
                kravetTilAlderOgMinsteinntektErOppfylt(it)
            }
        }
}
