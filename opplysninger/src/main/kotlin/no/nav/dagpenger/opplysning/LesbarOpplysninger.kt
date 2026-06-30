package no.nav.dagpenger.opplysning

import java.time.LocalDate
import java.util.UUID

interface LesbarOpplysninger {
    val id: UUID

    val kunEgne: LesbarOpplysninger

    fun <T : Any> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>

    fun <T : Any> finnOpplysning(
        opplysningstype: Opplysningstype<T>,
        gjelderFor: LocalDate,
    ): Opplysning<T> = forDato(gjelderFor).finnOpplysning(opplysningstype)

    fun <T : Any> finnNullableOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>?

    fun <T : Any> har(opplysningstype: Opplysningstype<T>): Boolean

    fun <T : Any> har(
        opplysningstype: Opplysningstype<T>,
        gjelderFor: LocalDate,
    ): Boolean = forDato(gjelderFor).har(opplysningstype)

    fun <T : Any> mangler(opplysningstype: Opplysningstype<T>): Boolean = !har(opplysningstype)

    fun finnFlere(opplysningstyper: List<Opplysningstype<*>>): List<Opplysning<*>>

    fun <T : Any> finnAlle(opplysningstyper: List<Opplysningstype<T>>): List<Opplysning<T>>

    fun <T : Any> finnAlle(opplysningstype: Opplysningstype<T>): List<Opplysning<T>>

    fun finnOpplysning(opplysningId: UUID): Opplysning<*>

    fun forDato(gjelderFor: LocalDate): LesbarOpplysninger

    fun erSann(opplysningstype: Opplysningstype<Boolean>) = har(opplysningstype) && finnOpplysning(opplysningstype).verdi

    fun erSann(
        opplysningstype: Opplysningstype<Boolean>,
        gjelderFor: LocalDate,
    ) = har(opplysningstype, gjelderFor) && finnOpplysning(opplysningstype, gjelderFor).verdi

    fun oppfyller(opplysningstype: Opplysningstype<Boolean>) = erSann(opplysningstype)

    fun erErstattet(opplysninger: List<Opplysning<*>>): Boolean

    fun erErstattet(opplysningId: UUID): Boolean

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
