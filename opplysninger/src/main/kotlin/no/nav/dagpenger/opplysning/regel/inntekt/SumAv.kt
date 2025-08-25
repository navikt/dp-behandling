package no.nav.dagpenger.opplysning.regel.inntekt

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.verdier.Beløp
import java.time.LocalDate

class SumAv internal constructor(
    produserer: Opplysningstype<Beløp>,
    private val beløp: List<Opplysningstype<Beløp>>,
) : Regel<Beløp>(produserer, beløp) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Beløp {
        val verdier = beløp.map { opplysningstype -> opplysninger.finnOpplysning(opplysningstype).verdi }
        return verdier.fold(Beløp(0.0)) { acc, beløp -> acc + beløp }
    }

    override fun toString() = "Summerer ${beløp.joinToString(" + ") { it.navn }}"
}

fun Opplysningstype<Beløp>.sumAv(vararg beløp: Opplysningstype<Beløp>) = SumAv(this, beløp.toList())
