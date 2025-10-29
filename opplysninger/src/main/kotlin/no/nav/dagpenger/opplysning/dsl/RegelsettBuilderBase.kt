package no.nav.dagpenger.opplysning.dsl

import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Hjemmel
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

abstract class RegelsettBuilderBase(
    protected val hjemmel: Hjemmel,
    protected val type: RegelsettType,
) {
    protected val regler: MutableMap<Opplysningstype<*>, TemporalCollection<Regel<*>>> = mutableMapOf()
    protected val avklaringer: MutableSet<Avklaringkode> = mutableSetOf()
    protected var skalKjøres: (opplysninger: LesbarOpplysninger) -> Boolean = { true }
    protected var relevant: (opplysninger: LesbarOpplysninger) -> Boolean = { true }

    fun avklaring(avklaringkode: Avklaringkode) {
        avklaringer.add(avklaringkode)
    }

    fun skalVurderes(block: (opplysninger: LesbarOpplysninger) -> Boolean) {
        skalKjøres = block
    }

    fun påvirkerResultat(block: (opplysninger: LesbarOpplysninger) -> Boolean) {
        relevant = block
    }

    fun <T : Comparable<T>> regel(
        produserer: Opplysningstype<T>,
        gjelderFraOgMed: LocalDate = LocalDate.MIN,
        block: Opplysningstype<T>.() -> Regel<*>,
    ) = leggTil(gjelderFraOgMed, produserer.block())

    private fun leggTil(
        gjelderFra: LocalDate,
        regel: Regel<*>,
    ) {
        regel.regelsettnavn = hjemmel.kortnavn
        regler.computeIfAbsent(regel.produserer) { TemporalCollection() }.put(gjelderFra, regel)
    }

    abstract fun build(): Regelsett
}
