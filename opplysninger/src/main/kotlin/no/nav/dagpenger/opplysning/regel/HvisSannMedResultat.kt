package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class HvisSannMedResultat<T : Any>(
    produserer: Opplysningstype<T>,
    private val sjekk: Opplysningstype<Boolean>,
    private val hvisSann: Opplysningstype<T>,
    private val hvisUsann: Opplysningstype<T>,
) : Regel<T>(produserer, listOf(sjekk, hvisSann, hvisUsann)) {
    // Grenen som ikke velges er verken avhengighet, grunnlag eller behov
    override fun aktiveAvhengigheter(opplysninger: LesbarOpplysninger): List<Opplysningstype<out Any>> =
        when {
            opplysninger.mangler(sjekk) -> listOf(sjekk)
            opplysninger.finnOpplysning(sjekk).verdi -> listOf(sjekk, hvisSann)
            else -> listOf(sjekk, hvisUsann)
        }

    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): T {
        val sjekk = opplysninger.finnOpplysning(sjekk).verdi

        return if (sjekk) {
            opplysninger.finnOpplysning(hvisSann).verdi
        } else {
            opplysninger.finnOpplysning(hvisUsann).verdi
        }
    }

    override fun toString() = "Hvis $sjekk er sann, returner $hvisSann, ellers returner $hvisUsann"
}

fun <T : Any> Opplysningstype<T>.hvisSannMedResultat(
    sjekk: Opplysningstype<Boolean>,
    hvisSann: Opplysningstype<T>,
    hvisUsann: Opplysningstype<T>,
) = HvisSannMedResultat(this, sjekk, hvisSann, hvisUsann)
