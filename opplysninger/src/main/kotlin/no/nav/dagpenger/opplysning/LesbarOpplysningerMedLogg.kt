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

    override fun <T : Any> finnOpplysning(opplysningstype: Opplysningstype<T>) =
        opplysninger.finnOpplysning(opplysningstype).apply {
            oppslag.add(this)
        }

    override fun <T : Any> finnNullableOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>? =
        opplysninger.finnNullableOpplysning(opplysningstype)?.apply {
            oppslag.add(this)
        }

    override fun finnOpplysning(opplysningId: UUID) =
        opplysninger.finnOpplysning(opplysningId).apply {
            oppslag.add(this)
        }

    override fun <T : Any> har(opplysningstype: Opplysningstype<T>) =
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

    override fun finnFlere(opplysningstyper: List<Opplysningstype<*>>) = TODO("Not yet implemented")

    override fun <T : Any> finnAlle(opplysningstyper: List<Opplysningstype<T>>) = TODO("Not yet implemented")

    override fun <T : Any> finnAlle(opplysningstype: Opplysningstype<T>) = TODO("Not yet implemented")

    override fun erErstattet(opplysninger: List<Opplysning<*>>) = TODO("Not yet implemented")

    override val kunEgne: Opplysninger get() = TODO("Not yet implemented")

    override fun somListe(filter: LesbarOpplysninger.Filter) = TODO("Not yet implemented")

    override fun forDato(gjelderFor: LocalDate) = TODO()
}
