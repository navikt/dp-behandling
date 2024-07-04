package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.behandling.konklusjon.KonklusjonsSjekk.Resultat.IkkeKonkludert
import no.nav.dagpenger.behandling.konklusjon.KonklusjonsSjekk.Resultat.Konkludert
import no.nav.dagpenger.behandling.konklusjon.KonklusjonsStrategi
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.alle

object KravPåDagpenger {
    val kravPåDagpenger = Opplysningstype.somBoolsk("Krav på dagpenger")
    val regelsett =
        Regelsett("Krav på dagpenger") {
            regel(kravPåDagpenger) {
                alle(
                    Alderskrav.kravTilAlder,
                    Minsteinntekt.minsteinntekt,
                    ReellArbeidssøker.kravTilArbeidssøker,
                    Meldeplikt.registrertPåSøknadstidspunktet,
                    Rettighetstype.rettighetstype,
                )
            }
        }

    val Innvilgelse =
        KonklusjonsStrategi(DagpengerKonklusjoner.Innvilgelse) { opplysninger ->
            if (opplysninger.mangler(kravPåDagpenger)) return@KonklusjonsStrategi IkkeKonkludert
            if (opplysninger.finnOpplysning(kravPåDagpenger).verdi) {
                return@KonklusjonsStrategi Konkludert
            } else {
                IkkeKonkludert
            }
        }

    val HattLukkedeSakerSiste8UkerKontroll =
        Kontrollpunkt(Avklaringspunkter.HattLukkedeSakerSiste8Uker) { it.har(kravPåDagpenger) }
}
