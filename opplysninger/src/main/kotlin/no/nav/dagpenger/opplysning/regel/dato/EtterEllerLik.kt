package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class EtterEllerLik internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val er: Opplysningstype<LocalDate>,
    private val etter: Opplysningstype<LocalDate>,
) : Regel<Boolean>(produserer, listOf(er, etter)) {
    override fun kjør(opplysninger: LesbarOpplysninger): Boolean {
        val a = opplysninger.finnOpplysning(er).verdi
        val b = opplysninger.finnOpplysning(etter).verdi
        return b.isAfter(a) || a.isEqual(b)
    }

    override fun toString() = "Sjekker at $er er etter eller lik $etter"
}

fun Opplysningstype<Boolean>.etterEllerLik(
    er: Opplysningstype<LocalDate>,
    etter: Opplysningstype<LocalDate>,
) = EtterEllerLik(this, er, etter)
