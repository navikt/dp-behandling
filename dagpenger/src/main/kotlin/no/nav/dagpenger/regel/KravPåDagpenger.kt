package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.tekstId
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt

object KravPåDagpenger {
    val kravPåDagpenger = Opplysningstype.somBoolsk("Krav på dagpenger".tekstId("opplysning.krav-paa-dagpenger"))
    val minsteinntektEllerVerneplikt = Opplysningstype.somBoolsk("Oppfyller kravet til minsteinntekt eller verneplikt")

    val regelsett =
        Regelsett("Krav på dagpenger") {

            regel(minsteinntektEllerVerneplikt) { enAv(minsteinntekt, oppfyllerKravetTilVerneplikt) }

            regel(kravPåDagpenger) {
                alle(
                    Alderskrav.kravTilAlder,
                    FulleYtelser.ikkeFulleYtelser,
                    Medlemskap.oppfyllerMedlemskap,
                    Meldeplikt.registrertPåSøknadstidspunktet,
                    minsteinntektEllerVerneplikt,
                    Opphold.oppfyllerKravet,
                    ReellArbeidssøker.kravTilArbeidssøker,
                    Rettighetstype.rettighetstype,
                    Samordning.utfallEtterSamordning,
                    StreikOgLockout.ikkeStreikEllerLockout,
                    TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntektOgArbeidstid,
                    Utdanning.kravTilUtdanning,
                    Utestengning.oppfyllerKravetTilIkkeUtestengt,
                )
            }
        }
}
