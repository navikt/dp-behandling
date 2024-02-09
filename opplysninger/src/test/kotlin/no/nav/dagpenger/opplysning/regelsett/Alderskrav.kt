package no.nav.dagpenger.opplysning.regelsett

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.oppslag
import java.time.LocalDate

object Alderskrav {
    val fødselsdato = Opplysningstype<LocalDate>("Fødselsdato")
    val aldersgrense = Opplysningstype<Int>("Aldersgrense")

    // TODO: Bør vi aliase slik? Da er det veldig tydelig hvor opplysningene kommer fra
    // val virkningsdato = Opplysningstype<LocalDate>("Virkningsdato")
    val virkningsdato = Virkningsdato.virkningsdato

    private val sisteMåned = Opplysningstype<LocalDate>("Dato søker når maks alder")
    private val sisteDagIMåned = Opplysningstype<LocalDate>("Siste mulige dag bruker kan oppfylle alderskrav")

    val vilkår = Opplysningstype<Boolean>("Oppfyller kravet til alder")

    val regelsett =
        Regelsett("alder").apply {
            regel { aldersgrense.oppslag(virkningsdato) { 67 } }
            regel { sisteMåned.leggTilÅr(fødselsdato, aldersgrense) }
            regel { sisteDagIMåned.sisteDagIMåned(sisteMåned) }
            regel { vilkår.førEllerLik(virkningsdato, sisteDagIMåned) }
        }
}