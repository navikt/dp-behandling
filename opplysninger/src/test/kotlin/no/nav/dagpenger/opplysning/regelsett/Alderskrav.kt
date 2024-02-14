package no.nav.dagpenger.opplysning.regelsett

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.oppslag

internal object Alderskrav {
    val fødselsdato = Opplysningstype.somDato("Fødselsdato")
    val aldersgrense = Opplysningstype.somHeltall("Aldersgrense")

    private val virkningsdato = Virkningsdato.virkningsdato
    private val sisteMåned = Opplysningstype.somDato("Dato søker når maks alder")
    private val sisteDagIMåned = Opplysningstype.somDato("Siste mulige dag bruker kan oppfylle alderskrav")

    val vilkår = Opplysningstype.somBoolsk("Oppfyller kravet til alder")

    val regelsett =
        Regelsett("alder") {
            regel(aldersgrense) { oppslag(virkningsdato) { 67 } }
            regel(sisteMåned) { leggTilÅr(fødselsdato, aldersgrense) }
            regel(sisteDagIMåned) { sisteDagIMåned(sisteMåned) }
            regel(vilkår) { førEllerLik(virkningsdato, sisteDagIMåned) }
        }
}
