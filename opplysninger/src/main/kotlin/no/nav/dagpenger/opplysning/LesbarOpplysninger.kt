package no.nav.dagpenger.opplysning

import java.time.LocalDate
import java.util.UUID

interface LesbarOpplysninger {
    val id: UUID

    val kunEgne: Opplysninger

    fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>

    fun har(opplysningstype: Opplysningstype<*>): Boolean

    fun mangler(opplysningstype: Opplysningstype<*>): Boolean = !har(opplysningstype)

    fun finnFlere(opplysningstyper: List<Opplysningstype<*>>): List<Opplysning<*>>

    fun <T : Comparable<T>> finnAlle(opplysningstyper: List<Opplysningstype<T>>): List<Opplysning<T>>

    fun <T : Comparable<T>> finnAlle(opplysningstype: Opplysningstype<T>): List<Opplysning<T>>

    fun finnOpplysning(opplysningId: UUID): Opplysning<*>

    fun forDato(gjelderFor: LocalDate): LesbarOpplysninger

    fun erSann(opplysningstype: Opplysningstype<Boolean>) = har(opplysningstype) && finnOpplysning(opplysningstype).verdi

    fun oppfyller(opplysningstype: Opplysningstype<Boolean>) = erSann(opplysningstype)

    fun erErstattet(opplysning: Opplysning<*>): Boolean

    fun somListe(filter: Filter = Filter.Alle): List<Opplysning<*>>

    companion object {
        fun Collection<Opplysning<*>>.somOpplysninger() = Opplysninger.med(this)
    }

    enum class Filter {
        Alle,
        Egne,
    }
}

typealias Opplysningssjekk = (LesbarOpplysninger) -> Boolean
