package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.verdier.Beløp
import java.math.BigDecimal
import java.time.LocalDate

class Avrund(
    produserer: Opplysningstype<Beløp>,
    private val beløp: Opplysningstype<Beløp>,
) : Regel<Beløp>(produserer, listOf(beløp)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): Beløp {
        val verdi = opplysninger.finnOpplysning(beløp).verdi
        return Beløp(verdi.avrundet.numberValueExact(BigDecimal::class.java))
    }

    override fun toString() = "Produserer $produserer ved å avrunde $beløp"
}

fun Opplysningstype<Beløp>.avrund(grunnlag: Opplysningstype<Beløp>) = Avrund(this, grunnlag)
