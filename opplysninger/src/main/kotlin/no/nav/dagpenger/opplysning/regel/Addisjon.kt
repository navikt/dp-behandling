package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp
import java.time.LocalDate

class Addisjon(
    produserer: Opplysningstype<Beløp>,
    private val ledd: List<Opplysningstype<Beløp>>,
) : Regel<Beløp>(produserer, ledd) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Beløp {
        val verdier = ledd.map { opplysninger.finnOpplysning(it).verdi }
        return Beløp(verdier.sumOf { it.verdien })
    }

    override fun toString(): String = "Addisjon av ${ledd.joinToString("+") { it.toString() }}"
}

fun Opplysningstype<Beløp>.addisjon(
    ledd1: Opplysningstype<Beløp>,
    ledd2: Opplysningstype<Beløp>,
) = Addisjon(
    this,
    listOf(ledd1, ledd2),
)

fun Opplysningstype<Beløp>.addisjon(vararg ledd: Opplysningstype<Beløp>) =
    Addisjon(
        this,
        ledd.toList(),
    )
