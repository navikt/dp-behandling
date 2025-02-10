package no.nav.dagpenger.regel

import no.nav.dagpenger.avklaring.Kontrollpunkt
import no.nav.dagpenger.opplysning.Opplysningsformål
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.aldriSynlig
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.dato
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.heltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.regel.Alderskrav.sisteDagIMåned
import no.nav.dagpenger.regel.OpplysningsTyper.AldersgrenseId
import no.nav.dagpenger.regel.OpplysningsTyper.FødselsdatoId
import no.nav.dagpenger.regel.OpplysningsTyper.KravTilAlderId
import no.nav.dagpenger.regel.OpplysningsTyper.SisteDagIMånedId
import no.nav.dagpenger.regel.OpplysningsTyper.SisteMånedId
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadsdato

object Alderskrav {
    val fødselsdato = Opplysningstype.dato(FødselsdatoId, "Fødselsdato", Opplysningsformål.Bruker)

    private val prøvingsdato = Søknadstidspunkt.prøvingsdato

    private val aldersgrense = heltall(AldersgrenseId, "Aldersgrense", synlig = aldriSynlig)
    private val sisteMåned = dato(SisteMånedId, "Dato søker når maks alder", synlig = aldriSynlig)
    private val sisteDagIMåned = dato(SisteDagIMånedId, "Siste mulige dag bruker kan oppfylle alderskrav")

    val kravTilAlder = boolsk(KravTilAlderId, "Oppfyller kravet til alder")

    val regelsett =
        vilkår(
            folketrygden.hjemmel(4, 23, "Bortfall på grunn av alder", "Alder"),
        ) {
            regel(fødselsdato) { innhentes }
            regel(aldersgrense) { oppslag(prøvingsdato) { 67 } }
            regel(sisteMåned) { leggTilÅr(fødselsdato, aldersgrense) }
            regel(sisteDagIMåned) { sisteDagIMåned(sisteMåned) }

            utfall(kravTilAlder) { førEllerLik(prøvingsdato, sisteDagIMåned) }
        }

    val MuligGjenopptakKontroll =
        Kontrollpunkt(Avklaringspunkter.MuligGjenopptak) { it.har(kravTilAlder) && it.finnOpplysning(kravTilAlder).verdi }

    val HattLukkedeSakerSiste8UkerKontroll =
        Kontrollpunkt(Avklaringspunkter.HattLukkedeSakerSiste8Uker) { it.har(kravTilAlder) && it.finnOpplysning(kravTilAlder).verdi }

    val Under18Kontroll =
        Kontrollpunkt(Avklaringspunkter.BrukerUnder18) {
            if (it.mangler(fødselsdato) || it.mangler(søknadsdato)) {
                return@Kontrollpunkt false
            }
            val søknadsdato = it.finnOpplysning(søknadsdato).verdi
            val fødselsdato = it.finnOpplysning(fødselsdato).verdi

            søknadsdato.minusYears(18).isBefore(fødselsdato)
        }

    val TilleggsopplysningsKontroll = Kontrollpunkt(Avklaringspunkter.HarTilleggsopplysninger) { it.har(søknadIdOpplysningstype) }
}
