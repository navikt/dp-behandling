package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp
import java.time.LocalDate

class Divisjon<T : Comparable<T>> internal constructor(
    produserer: Opplysningstype<Beløp>,
    private val beløp: Opplysningstype<Beløp>,
    private val faktor: Opplysningstype<T>,
    private val operasjon: (Beløp, T) -> Beløp,
) : Regel<Beløp>(produserer, listOf(beløp, faktor)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Beløp {
        val a = opplysninger.finnOpplysning(beløp).verdi
        val b = opplysninger.finnOpplysning(faktor).verdi
        return operasjon.invoke(a, b)
    }

    override fun toString(): String = "Divisjon av $beløp med $faktor"
}

@JvmName("divisjonDouble")
fun Opplysningstype<Beløp>.divisjon(
    beløp: Opplysningstype<Beløp>,
    faktor: Opplysningstype<Double>,
) = Divisjon(this, beløp, faktor) { a, b -> a / b }

@JvmName("divisjonInt")
fun Opplysningstype<Beløp>.divisjon(
    beløp: Opplysningstype<Beløp>,
    faktor: Opplysningstype<Int>,
) = Divisjon(this, beløp, faktor) { a, b -> a / b }
