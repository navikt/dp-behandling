package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

class LeggTilUker internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
    private val antallUker: Opplysningstype<Int>,
) : Regel<LocalDate>(produserer, listOf(dato, antallUker)) {
    override fun kjør(opplysninger: LesbarOpplysninger): LocalDate {
        val a = opplysninger.finnOpplysning(dato).verdi
        return a.plusWeeks(opplysninger.finnOpplysning(antallUker).verdi.toLong())
    }

    override fun toString() = "Legg til $antallUker uker på $dato"
}

fun Opplysningstype<LocalDate>.leggTilUker(
    dato: Opplysningstype<LocalDate>,
    antallUker: Opplysningstype<Int>,
) = LeggTilUker(this, dato, antallUker)
