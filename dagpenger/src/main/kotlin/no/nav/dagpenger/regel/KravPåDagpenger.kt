package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
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
                    Minsteinntekt.minsteinntekt,
                    ReellArbeidssøker.kravTilArbeidssøker,
                    Meldeplikt.registrertPåSøknadstidspunktet,
                    Rettighetstype.rettighetstype,
                    Utdanning.kravTilUtdanning,
                    Utestengning.ikkeUtestengt,
                    StreikOgLockout.ikkeStreikEllerLockout,
                    Medlemskap.oppfyllerMedlemskap,
                    TapAvArbeidsinntektOgArbeidstid.kravTilTapAvArbeidsinntektOgArbeidstid,
                )
            }
        }

    val Totrinnskontroll =
        Kontrollpunkt(Avklaringspunkter.Totrinnskontroll) { it.har(kravPåDagpenger) && it.finnOpplysning(kravPåDagpenger).verdi }
}
