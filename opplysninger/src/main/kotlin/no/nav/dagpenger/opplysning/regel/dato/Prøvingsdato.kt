package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class Prøvingsdato internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer, listOf(dato)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ) = opplysninger.finnOpplysning(dato).verdi

    override fun toString() = "Fastsetter $produserer med verdi $dato og gyldighetsperiode fom $dato"
}

fun Opplysningstype<LocalDate>.prøvingsdato(dato: Opplysningstype<LocalDate>) = Prøvingsdato(this, dato)
