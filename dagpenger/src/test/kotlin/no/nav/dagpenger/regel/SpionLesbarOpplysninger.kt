package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate
import java.util.UUID

/**
 * Sporer hvilke opplysningstyper kontrollpunktets lambda faktisk aksesserer.
 *
 * Designvalg:
 * - har() → alltid true: hindrer early returns av typen "if (!har(x)) return false"
 * - erSann() → alltid false: hindrer early returns av typen "if (erSann(x)) return false",
 *   og overskriver default-implementasjonen som ville kalt finnOpplysning()
 * - finnOpplysning() → kaster SpionAvsluttet etter å ha tracket typen
 * - finnNullableOpplysning() → returnerer null etter å ha tracket typen
 *
 * Tilnærmingen er "best effort": fanger alle har()/erSann()-guards og
 * den første finnOpplysning()-kallet. Early returns som avhenger av
 * erSann() == true vil ikke utforskes videre.
 */
class SpionLesbarOpplysninger : LesbarOpplysninger {
    private val _brukedeTyper = mutableSetOf<Opplysningstype<*>>()
    val brukedeTyper: Set<Opplysningstype<*>> get() = _brukedeTyper.toSet()

    override val id: UUID = UUID.randomUUID()
    override val kunEgne: LesbarOpplysninger get() = this

    override fun <T : Any> har(opplysningstype: Opplysningstype<T>): Boolean {
        _brukedeTyper.add(opplysningstype)
        return true
    }

    override fun erSann(opplysningstype: Opplysningstype<Boolean>): Boolean {
        _brukedeTyper.add(opplysningstype)
        return false
    }

    override fun erSann(
        opplysningstype: Opplysningstype<Boolean>,
        gjelderFor: LocalDate,
    ): Boolean {
        _brukedeTyper.add(opplysningstype)
        return false
    }

    override fun <T : Any> finnNullableOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>? {
        _brukedeTyper.add(opplysningstype)
        return null
    }

    override fun <T : Any> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> {
        _brukedeTyper.add(opplysningstype)
        throw SpionAvsluttet()
    }

    override fun finnOpplysning(opplysningId: UUID): Opplysning<*> = throw SpionAvsluttet()

    override fun forDato(gjelderFor: LocalDate): LesbarOpplysninger = this

    override fun finnFlere(opplysningstyper: List<Opplysningstype<*>>): List<Opplysning<*>> {
        _brukedeTyper.addAll(opplysningstyper)
        return emptyList()
    }

    override fun <T : Any> finnAlle(opplysningstyper: List<Opplysningstype<T>>): List<Opplysning<T>> {
        _brukedeTyper.addAll(opplysningstyper)
        return emptyList()
    }

    override fun <T : Any> finnAlle(opplysningstype: Opplysningstype<T>): List<Opplysning<T>> {
        _brukedeTyper.add(opplysningstype)
        return emptyList()
    }

    override fun erErstattet(opplysninger: List<Opplysning<*>>): Boolean = false

    override fun somListe(filter: LesbarOpplysninger.Filter): List<Opplysning<*>> = emptyList()

    class SpionAvsluttet : Exception()
}
