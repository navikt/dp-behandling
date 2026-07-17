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
    val kvoter: List<KvoteDefinisjon> = emptyList(),
    // Er sann når regelsettet besvarer en selvstendig søknad (jf. forvaltningsloven § 11 a,
    // plikten til å svare på en søknad) – altså et spørsmål bruker selv har bedt om å få vurdert,
    // til forskjell fra vilkår som rutinemessig inngår i den løpende vurderingen av rettighetsforholdet.
    private val selvstendigSøknad: ((opplysninger: LesbarOpplysninger) -> Boolean)? = null,
) {
    val navn: String = hjemmel.kortnavn

    // Hvilke opplysninger dette regelsettet definerer til vedtak
    val ønsketInformasjon: Set<Opplysningstype<*>>
        get() =
            buildSet {
                addAll(ønsketResultat)
                addAll(betingelser)
                utfall?.let { add(it) }
            }

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

    // Bruker har fått avslag på en selvstendig søknad dette regelsettet besvarer
    fun girAvslagPåSelvstendigSøknad(opplysninger: LesbarOpplysninger): Boolean =
        selvstendigSøknad?.invoke(opplysninger) == true && utfall != null && !opplysninger.erSann(utfall)

    override fun toString() = "Regelsett(navn=$navn, type=$type)"
}
