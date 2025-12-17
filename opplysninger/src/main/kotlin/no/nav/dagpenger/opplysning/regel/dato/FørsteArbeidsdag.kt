package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.dato.finnFørsteArbeidsdag
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

/**
 *  Regel som finner første *arbeidsdag* for en dato
 *
 *  En arbeidsdag er en dag som ikke er helg (lørdag eller søndag) eller en helligdag for Norge
 *
 *  - Hvis datoen er en arbeidsdag returneres datoen
 *  - Hvis datoen er en helg eller helligdag returneres den første arbeidsdagen etter datoen
 *
 */
class FørsteArbeidsdag internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer, listOf(dato)) {
    override fun kjør(opplysninger: LesbarOpplysninger): LocalDate {
        val arbeidsdag = opplysninger.finnOpplysning(dato).verdi
        return finnFørsteArbeidsdag(arbeidsdag)
    }

    override fun toString() = "Finn første virkedag etter $dato"
}

fun Opplysningstype<LocalDate>.førsteArbeidsdag(dato: Opplysningstype<LocalDate>) = FørsteArbeidsdag(this, dato)
