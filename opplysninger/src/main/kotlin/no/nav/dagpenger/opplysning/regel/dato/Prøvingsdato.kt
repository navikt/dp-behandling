package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class Prøvingsdato internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer, listOf(dato)) {
    override fun kjør(opplysninger: LesbarOpplysninger) = opplysninger.finnOpplysning(dato).verdi

    override fun gyldighetsperiode(basertPå: List<Opplysning<*>>): Gyldighetsperiode =
        Gyldighetsperiode(basertPå.single().verdi as LocalDate)

    override fun toString() = "Fastsetter $produserer med verdi $dato og gyldighetsperiode fom $dato"
}

fun Opplysningstype<LocalDate>.prøvingsdato(dato: Opplysningstype<LocalDate>) = Prøvingsdato(this, dato)
