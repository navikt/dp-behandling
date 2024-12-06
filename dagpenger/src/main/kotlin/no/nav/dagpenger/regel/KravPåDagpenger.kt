package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.tekstId

object KravPåDagpenger {
    val kravPåDagpenger = Opplysningstype.somBoolsk("Krav på dagpenger".tekstId("opplysning.krav-paa-dagpenger"))
    val regelsett =
        Regelsett("Krav på dagpenger") {
            regel(kravPåDagpenger) {
                alle(
                    Alderskrav.kravTilAlder,
                    FulleYtelser.ikkeFulleYtelser,
                    Medlemskap.oppfyllerMedlemskap,
                    Meldeplikt.registrertPåSøknadstidspunktet,
                    Minsteinntekt.minsteinntekt,
                    Opphold.oppfyllerKravet,
                    ReellArbeidssøker.kravTilArbeidssøker,
                    Rettighetstype.rettighetstype,
                    Samordning.utfallEtterSamordning,
                    StreikOgLockout.ikkeStreikEllerLockout,
                    TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntektOgArbeidstid,
                    Utdanning.kravTilUtdanning,
                    Utestengning.ikkeUtestengt,
                )
            }
        }
}
