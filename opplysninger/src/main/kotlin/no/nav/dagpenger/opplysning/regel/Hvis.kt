package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel

class HvisRegel<T : Comparable<T>>(
    private val produkt: Opplysningstype<T>,
    private val boolsk: Opplysningstype<Boolean>,
    private val verdi: Opplysningstype<T>,
    private val fjasbhengel: T,
) : Regel<T>(produkt, listOf(boolsk, verdi)) {
    override fun kjør(opplysninger: LesbarOpplysninger): T {
        val boolskVerdi = opplysninger.finnOpplysning(boolsk).verdi
        val verdi = opplysninger.finnOpplysning(verdi).verdi

        return if (boolskVerdi) {
            verdi
        } else {
            fjasbhengel
        }
    }

    override fun toString(): String {
        TODO("Not yet implemented")
    }
}

fun <T : Comparable<T>> Opplysningstype<T>.hvis(
    boolsk: Opplysningstype<Boolean>,
    verdi: Opplysningstype<T>,
    default: T,
): HvisRegel<T> = HvisRegel(this, boolsk, verdi, default)
