package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class SisteDagIMåned internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer, listOf(dato)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): LocalDate {
        val a = opplysninger.finnOpplysning(dato).verdi
        return a.withDayOfMonth(a.lengthOfMonth())
    }

    override fun toString() = "Fastsetter $produserer til siste dag i måneden for $dato"
}

fun Opplysningstype<LocalDate>.sisteDagIMåned(dato: Opplysningstype<LocalDate>) = SisteDagIMåned(this, dato)
