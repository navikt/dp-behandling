package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

internal sealed interface Avhengigheter {
    val mulige: List<Opplysningstype<*>>
    val kontroller: List<Opplysningstype<*>> get() = emptyList()

    fun løs(
        opplysninger: LesbarOpplysninger,
        avventer: (Opplysningstype<*>) -> Boolean = { false },
    ): LøsteAvhengigheter

    data class Faste(
        override val mulige: List<Opplysningstype<*>>,
    ) : Avhengigheter {
        override fun løs(
            opplysninger: LesbarOpplysninger,
            avventer: (Opplysningstype<*>) -> Boolean,
        ) = LøsteAvhengigheter.Avklart(mulige)
    }

    data class Betinget(
        val sjekk: Opplysningstype<Boolean>,
        val hvisSann: List<Opplysningstype<*>>,
        val hvisUsann: List<Opplysningstype<*>>,
    ) : Avhengigheter {
        override val kontroller = listOf(sjekk)
        override val mulige = (kontroller + hvisSann + hvisUsann).distinct()

        override fun løs(
            opplysninger: LesbarOpplysninger,
            avventer: (Opplysningstype<*>) -> Boolean,
        ): LøsteAvhengigheter {
            if (opplysninger.mangler(sjekk) || avventer(sjekk)) return LøsteAvhengigheter.Uavklart(kontroller)
            val gren = if (opplysninger.finnOpplysning(sjekk).verdi) hvisSann else hvisUsann
            return LøsteAvhengigheter.Avklart((kontroller + gren).distinct())
        }
    }
}

internal sealed interface LøsteAvhengigheter {
    val nødvendige: List<Opplysningstype<*>>

    data class Avklart(
        override val nødvendige: List<Opplysningstype<*>>,
    ) : LøsteAvhengigheter

    data class Uavklart(
        override val nødvendige: List<Opplysningstype<*>>,
    ) : LøsteAvhengigheter
}
