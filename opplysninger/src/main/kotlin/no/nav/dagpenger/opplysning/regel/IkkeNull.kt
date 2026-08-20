package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDate

class IkkeNull internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val a: Opplysningstype<Int>,
) : Regel<Boolean>(produserer, listOf(a)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Boolean {
        val a = opplysninger.finnOpplysning(a).verdi
        return a > 0
    }

    override fun toString() = "Sjekker om $a er større enn 0"
}

fun Opplysningstype<Boolean>.ikkeNull(er: Opplysningstype<Int>) = IkkeNull(this, er)
