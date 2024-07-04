package no.nav.dagpenger.opplysning

import java.time.LocalDateTime
import java.util.UUID

// Sporer hvilke opplysninger som har vært i bruk
class LesbarOpplysningerMedLogg(
    private val opplysninger: LesbarOpplysninger,
) : LesbarOpplysninger {
    override val id: UUID = opplysninger.id

    private val oppslag = mutableListOf<Opplysning<*>>()

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

    override fun finnAlle(opplysningstyper: List<Opplysningstype<*>>) = TODO()

    override fun finnAlle() = TODO()
}
