package no.nav.dagpenger.regel.regelsett.fastsetting
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.dsl.fastsettelse
import no.nav.dagpenger.opplysning.folketrygden
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.opplysning.verdier.enhet.Enhet
import no.nav.dagpenger.regel.OpplysningsTyper.fastsattArbeidstidPerUkeFørTapId
import no.nav.dagpenger.regel.kravPåDagpenger
import no.nav.dagpenger.regel.oppfyllerKravetTilMinsteinntektEllerVerneplikt
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.ønsketArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.samordnetBeregnetArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.maksimalVanligArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.ordinærEllerVernepliktArbeidstid

object Vanligarbeidstid {
    val fastsattVanligArbeidstid =
        Opplysningstype.desimaltall(
            fastsattArbeidstidPerUkeFørTapId,
            "Fastsatt arbeidstid per uke før tap",
            enhet = Enhet.Timer,
        )
    val regelsett by lazy {
        fastsettelse(
            folketrygden.hjemmel(4, 3, "Fastsettelse av arbeidstid", "Fastsettelse av arbeidstid"),
        ) {
            skalVurderes { kravPåDagpenger(it, this@Vanligarbeidstid::class) }

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
}
