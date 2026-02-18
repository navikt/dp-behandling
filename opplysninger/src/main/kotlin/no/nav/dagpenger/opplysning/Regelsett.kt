package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.Ekstern
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

enum class RegelsettType {
    Vilkår,
    Fastsettelse,

    /** Prosess-regelsett produserer opplysninger som ikke arves til neste behandling i kjeden */
    Prosess,
}

class Regelsett internal constructor(
    val hjemmel: Hjemmel,
    val type: RegelsettType,
    private val ønsketResultat: List<Opplysningstype<*>>,
    private val regler: Map<Opplysningstype<*>, TemporalCollection<Regel<*>>>,
    val avklaringer: Set<Avklaringkode>,
    val utfall: Opplysningstype<Boolean>?,
    val skalKjøres: (opplysninger: LesbarOpplysninger) -> Boolean,
    val skalRevurderes: (opplysning: LesbarOpplysninger) -> Boolean,
    val påvirkerResultat: (opplysninger: LesbarOpplysninger) -> Boolean,
    val betingelser: List<Opplysningstype<Boolean>>,
) {
    val navn: String = hjemmel.kortnavn

    // Hvilke opplysninger dette regelsettet definerer til vedtak
    val ønsketInformasjon: List<Opplysningstype<*>>
        get() = ønsketResultat + betingelser + listOfNotNull(utfall)

    // Hvilke opplysninger dette regelsettet produserer
    val produserer: Set<Opplysningstype<*>> by lazy { regler.map { it.key }.toSet() }

    // Hvilke opplysninger dette regelsettet er avhengig av
    val avhengerAv: Set<Opplysningstype<*>> by lazy {
        regler.flatMap { it.value.getAll().flatMap { regel -> regel.avhengerAv } }.toSet().minus(produserer)
    }

    // Hvilke opplysninger dette regelsettet innhenter via behov
    val behov: List<Opplysningstype<*>> by lazy {
        regler.flatMap { it.value.getAll() }.filterIsInstance<Ekstern<*>>().map { it.produserer }
    }

    // Returnerer regler som er gyldige for en gitt dato
    fun regler(forDato: LocalDate = LocalDate.MIN) = regler.map { it.value.get(forDato) }.toList()

    override fun toString() = "Regelsett(navn=$navn, type=$type)"
}
