package no.nav.dagpenger.opplysning

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Sporer hvilke opplysninger som har vært i bruk
class LesbarOpplysningerMedLogg(
    private val opplysninger: LesbarOpplysninger,
) : LesbarOpplysninger {
    private val oppslag = mutableListOf<Opplysning<*>>()

    override val id get() = opplysninger.id

    val sistBrukteOpplysning: LocalDateTime
        get() =
            oppslag.maxOfOrNull { it.opprettet }
                ?: throw IllegalStateException("Ingen opplysninger har blitt brukt")

    override fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>) =
        opplysninger.finnOpplysning(opplysningstype).apply {
            oppslag.add(this)
        }

    override fun finnOpplysning(opplysningId: UUID) =
        opplysninger.finnOpplysning(opplysningId).apply {
            oppslag.add(this)
        }

    override fun har(opplysningstype: Opplysningstype<*>) =
        opplysninger.har(opplysningstype).also { harOpplysning ->
            if (harOpplysning) {
                oppslag.add(opplysninger.finnOpplysning(opplysningstype))
            }
        }

    override fun erSann(opplysningstype: Opplysningstype<Boolean>) =
        opplysninger.erSann(opplysningstype).also {
            if (opplysninger.har(opplysningstype)) {
                oppslag.add(opplysninger.finnOpplysning(opplysningstype))
            }
        }

    override val utenErstattet: Opplysninger get() = opplysninger.utenErstattet
    override val aktiveOpplysninger: Opplysninger get() = TODO("Not yet implemented")

    override fun finnAlle(opplysningstyper: List<Opplysningstype<*>>) = TODO()

    override fun finnAlle(vararg opplysningstyper: Opplysningstype<*>) = TODO()

    override fun finnAlle() = TODO()

    override fun forDato(gjelderFor: LocalDate) = TODO()
}
