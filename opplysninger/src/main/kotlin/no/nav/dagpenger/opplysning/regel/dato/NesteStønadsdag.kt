package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.dato.finnFørsteArbeidsdag
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

/**
 *  Regel som finner neste *stønadsdag* for en dato
 *
 *  - Hvis datoen er en arbeidsdag returneres datoen
 *  - Hvis datoen er en helg eller helligdag returneres den første arbeidsdagen etter datoen
 *
 */
class NesteStønadsdag internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer, listOf(dato)) {
    override fun kjør(opplysninger: LesbarOpplysninger): LocalDate {
        val arbeidsdag = opplysninger.finnOpplysning(dato).verdi
        return finnFørsteArbeidsdag(arbeidsdag.plusDays(1))
    }

    override fun toString() = "Finn første stønadsdag etter $dato"
}

fun Opplysningstype<LocalDate>.nesteStønadsdag(dato: Opplysningstype<LocalDate>) = NesteStønadsdag(this, dato)
