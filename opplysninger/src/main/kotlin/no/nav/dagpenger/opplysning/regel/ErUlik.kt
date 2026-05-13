package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp
import java.time.LocalDate

class ErUlik internal constructor(
    produserer: Opplysningstype<Boolean>,
    private val a: Opplysningstype<Beløp>,
    private val b: Opplysningstype<Beløp>,
) : Regel<Boolean>(produserer, listOf(a, b)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Boolean {
        val a = opplysninger.finnOpplysning(a).verdi
        val b = opplysninger.finnOpplysning(b).verdi
        return a != b
    }

    override fun toString() = "$a er ikke lik $b"
}

fun Opplysningstype<Boolean>.erUlik(
    a: Opplysningstype<Beløp>,
    b: Opplysningstype<Beløp>,
) = ErUlik(this, a, b)
