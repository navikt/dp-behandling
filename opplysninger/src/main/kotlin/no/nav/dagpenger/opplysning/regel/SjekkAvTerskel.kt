package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class SjekkAvTerskel internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val andel: Opplysningstype<Double>,
    private val total: Opplysningstype<Double>,
    private val terskel: Opplysningstype<Double>,
) : Regel<Boolean>(produserer, listOf(andel, total, terskel)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Boolean {
        val andel = opplysninger.finnOpplysning(andel).verdi
        val total = opplysninger.finnOpplysning(total).verdi
        val terskel = opplysninger.finnOpplysning(terskel).verdi

        return (1 - (andel / total)) * 100 >= terskel
    }

    override fun toString() = "$andel av $total er minst $terskel prosent"
}

fun Opplysningstype<Boolean>.prosentTerskel(
    andel: Opplysningstype<Double>,
    total: Opplysningstype<Double>,
    terskel: Opplysningstype<Double>,
) = SjekkAvTerskel(this, andel, total, terskel)
