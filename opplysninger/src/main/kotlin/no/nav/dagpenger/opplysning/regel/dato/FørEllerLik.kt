package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class FørEllerLik internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val dato: Opplysningstype<LocalDate>,
    private val tom: Opplysningstype<LocalDate>,
) : Regel<Boolean>(produserer, listOf(dato, tom)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Boolean {
        val a = opplysninger.finnOpplysning(dato).verdi
        val b = opplysninger.finnOpplysning(tom).verdi
        return a.isBefore(b) || a.isEqual(b)
    }

    override fun toString() = "$dato er før eller lik $tom"
}

fun Opplysningstype<Boolean>.førEllerLik(
    er: Opplysningstype<LocalDate>,
    førEllerLik: Opplysningstype<LocalDate>,
) = FørEllerLik(this, er, førEllerLik)
