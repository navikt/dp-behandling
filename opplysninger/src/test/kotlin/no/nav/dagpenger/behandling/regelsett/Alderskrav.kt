package no.nav.dagpenger.behandling.regelsett

import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett
import no.nav.dagpenger.behandling.regel.førEllerLik
import no.nav.dagpenger.behandling.regel.leggTilÅr
import no.nav.dagpenger.behandling.regel.oppslag
import no.nav.dagpenger.behandling.regel.sisteDagIMåned
import java.time.LocalDate

object Alderskrav {
    val fødselsdato = Opplysningstype<LocalDate>("Fødselsdato")
    val aldersgrense = Opplysningstype<Int>("Aldersgrense")
    val virkningsdato = Opplysningstype<LocalDate>("Virkningsdato")

    private val sisteMåned = Opplysningstype<LocalDate>("Dato søker når maks alder")
    private val sisteDagIMåned = Opplysningstype<LocalDate>("Siste mulige dag bruker kan oppfylle alderskrav")

    val vilkår = Opplysningstype<Boolean>("Oppfyller kravet til alder")

    val regelsett =
        Regelsett("alder").apply {
            oppslag(aldersgrense, virkningsdato) { 67 }
            leggTilÅr(sisteMåned, fødselsdato, aldersgrense)
            sisteDagIMåned(sisteDagIMåned, sisteMåned)
            førEllerLik(vilkår, virkningsdato, sisteDagIMåned)
        }
}