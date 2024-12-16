package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype

class HvisRegel<T : Comparable<T>>(
    produkt: Opplysningstype<T>,
    private val boolsk: Opplysningstype<Boolean>,
    private val verdi: Opplysningstype<T>,
    private val default: T,
) : Regel<T>(produkt, listOf(boolsk, verdi)) {
    override fun kjør(opplysninger: LesbarOpplysninger): T {
        val boolskVerdi = opplysninger.finnOpplysning(boolsk).verdi
        val verdi = opplysninger.finnOpplysning(verdi).verdi

        return if (boolskVerdi) {
            verdi
        } else {
            default
        }
    }

    override fun toString() = "Hvis $boolsk er sann, returner $verdi, ellers returner $default"
}

fun <T : Comparable<T>> Opplysningstype<T>.hvis(
    boolsk: Opplysningstype<Boolean>,
    hvisSann: Opplysningstype<T>,
    hvisUsann: T,
): HvisRegel<T> = HvisRegel(this, boolsk, hvisSann, hvisUsann)
