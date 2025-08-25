package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class LeggTilDager internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
    private val antallDager: Opplysningstype<Int>,
) : Regel<LocalDate>(produserer, listOf(dato, antallDager)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): LocalDate {
        val a = opplysninger.finnOpplysning(dato).verdi
        return a.plusDays(opplysninger.finnOpplysning(antallDager).verdi.toLong())
    }

    override fun toString() = "Legg til $antallDager dager på $dato"
}

fun Opplysningstype<LocalDate>.leggTilDager(
    dato: Opplysningstype<LocalDate>,
    antallDager: Opplysningstype<Int>,
) = LeggTilDager(this, dato, antallDager)
