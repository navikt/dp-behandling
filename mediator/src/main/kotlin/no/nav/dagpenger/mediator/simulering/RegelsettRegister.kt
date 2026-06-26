package no.nav.dagpenger.mediator.simulering

import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.regelverk.RegelverkRegistrering

class RegelsettRegister(
    regelverkRegistreringer: List<RegelverkRegistrering>,
) {
    private val regelverk: Map<RegelverkType, Regelverk> =
        regelverkRegistreringer.associate { it.regelverk.navn to it.regelverk }

    fun alleRegelverk(): List<Regelverk> = regelverk.values.toList()

    fun finnRegelverk(navn: String): Regelverk? = regelverk[RegelverkType(navn)]

    fun finnRegelsett(
        regelverkNavn: String,
        regelsettNavn: String,
    ): Pair<Regelverk, Regelsett>? {
        val rv = finnRegelverk(regelverkNavn) ?: return null
        val rs = rv.finnRegelsett(regelsettNavn) ?: return null
        return rv to rs
    }
}
