package no.nav.dagpenger.opplysning

import java.time.LocalDate
import java.util.UUID

interface LesbarOpplysninger {
    val id: UUID

    val kunEgne: Opplysninger

    fun <T : Any> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>

    fun <T : Any> finnNullableOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>?

    fun <T : Any> har(opplysningstype: Opplysningstype<T>): Boolean

    fun <T : Any> mangler(opplysningstype: Opplysningstype<T>): Boolean = !har(opplysningstype)

    fun finnFlere(opplysningstyper: List<Opplysningstype<*>>): List<Opplysning<*>>

    fun <T : Any> finnAlle(opplysningstyper: List<Opplysningstype<T>>): List<Opplysning<T>>

    fun <T : Any> finnAlle(opplysningstype: Opplysningstype<T>): List<Opplysning<T>>

    fun finnOpplysning(opplysningId: UUID): Opplysning<*>

    fun forDato(gjelderFor: LocalDate): LesbarOpplysninger

    fun erSann(opplysningstype: Opplysningstype<Boolean>) = har(opplysningstype) && finnOpplysning(opplysningstype).verdi

    fun oppfyller(opplysningstype: Opplysningstype<Boolean>) = erSann(opplysningstype)

    fun erErstattet(opplysninger: List<Opplysning<*>>): Boolean

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
