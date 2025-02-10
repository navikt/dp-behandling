package no.nav.dagpenger.opplysning.regelsett

import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Id
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.uuid.UUIDv7

internal object Alderskrav {
    val fødselsdato = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Fødselsdato")
    val aldersgrense = Opplysningstype.heltall(Id(UUIDv7.ny(), Heltall), "Aldersgrense")

    private val virkningsdato = Prøvingsdato.prøvingsdato
    private val sisteMåned = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Dato søker når maks alder")
    private val sisteDagIMåned = Opplysningstype.dato(Id(UUIDv7.ny(), Dato), "Siste mulige dag bruker kan oppfylle alderskrav")

    val vilkår = Opplysningstype.boolsk(Id(UUIDv7.ny(), Boolsk), "Oppfyller kravet til alder")

    val regelsett =
        vilkår("alder") {
            regel(fødselsdato) { innhentes }
            regel(aldersgrense) { oppslag(virkningsdato) { 67 } }
            regel(sisteMåned) { leggTilÅr(fødselsdato, aldersgrense) }
            regel(sisteDagIMåned) { sisteDagIMåned(sisteMåned) }
            regel(vilkår) { førEllerLik(virkningsdato, sisteDagIMåned) }
        }
}
