package no.nav.dagpenger.opplysning.regelsett

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.oppslag
import java.time.LocalDate

internal object Alderskrav {
    val fødselsdato = Opplysningstype<LocalDate>("Fødselsdato")
    val aldersgrense = Opplysningstype<Int>("Aldersgrense")

    private val virkningsdato = Virkningsdato.virkningsdato
    private val sisteMåned = Opplysningstype<LocalDate>("Dato søker når maks alder")
    private val sisteDagIMåned = Opplysningstype<LocalDate>("Siste mulige dag bruker kan oppfylle alderskrav")

    val vilkår = Opplysningstype<Boolean>("Oppfyller kravet til alder")

    val regelsett =
        Regelsett("alder") {
            regel(fødselsdato) { innhentMed() }
            regel(aldersgrense) { oppslag(virkningsdato) { 67 } }
            regel(sisteMåned) { leggTilÅr(fødselsdato, aldersgrense) }
            regel(sisteDagIMåned) { sisteDagIMåned(sisteMåned) }
            regel(vilkår) { førEllerLik(virkningsdato, sisteDagIMåned) }
        }
}
