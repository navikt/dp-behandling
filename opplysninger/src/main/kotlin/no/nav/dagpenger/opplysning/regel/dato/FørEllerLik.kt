package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class FørEllerLik internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val tom: Opplysningstype<LocalDate>,
) : Regel<Boolean>(produserer, listOf(tom)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Boolean {
        val b = opplysninger.finnOpplysning(tom).verdi
        return prøvingsdato.isBefore(b) || prøvingsdato.isEqual(b)
    }

    override fun toString() = "Sjekker at dato er før eller lik $tom"
}

fun Opplysningstype<Boolean>.førEllerLik(tom: Opplysningstype<LocalDate>) = FørEllerLik(this, tom)
