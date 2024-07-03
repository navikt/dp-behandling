package no.nav.dagpenger.regel

import no.nav.dagpenger.behandling.konklusjon.KonklusjonsSjekk.Resultat.IkkeKonkludert
import no.nav.dagpenger.behandling.konklusjon.KonklusjonsSjekk.Resultat.Konkludert
import no.nav.dagpenger.behandling.konklusjon.KonklusjonsStrategi
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.oppslag

object Alderskrav {
    val fødselsdato = Opplysningstype.somDato("Fødselsdato".id("Fødselsdato"))

    private val virkningsdato = Søknadstidspunkt.søknadstidspunkt

    private val aldersgrense = Opplysningstype.somHeltall("Aldersgrense")
    private val sisteMåned = Opplysningstype.somDato("Dato søker når maks alder")
    private val sisteDagIMåned = Opplysningstype.somDato("Siste mulige dag bruker kan oppfylle alderskrav")

    val kravTilAlder = Opplysningstype.somBoolsk("Oppfyller kravet til alder")

    val regelsett =
        Regelsett("Alder") {
            regel(fødselsdato) { innhentes }
            regel(aldersgrense) { oppslag(virkningsdato) { 67 } }
            regel(sisteMåned) { leggTilÅr(fødselsdato, aldersgrense) }
            regel(sisteDagIMåned) { sisteDagIMåned(sisteMåned) }
            regel(kravTilAlder) { førEllerLik(virkningsdato, sisteDagIMåned) }
        }

    val AvslagAlder =
        KonklusjonsStrategi(DagpengerKonklusjoner.AvslagAlder) { opplysninger ->
            if (opplysninger.mangler(kravTilAlder)) return@KonklusjonsStrategi IkkeKonkludert
            if (!opplysninger.finnOpplysning(kravTilAlder).verdi) {
                return@KonklusjonsStrategi Konkludert
            } else {
                IkkeKonkludert
            }
        }
}
