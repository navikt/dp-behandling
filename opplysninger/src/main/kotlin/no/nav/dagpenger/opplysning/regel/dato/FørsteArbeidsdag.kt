package no.nav.dagpenger.opplysning.regel.dato

import no.bekk.bekkopen.date.NorwegianDateUtil
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

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
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): LocalDate {
        val arbeidsdag = opplysninger.finnOpplysning(dato).verdi
        return finnFørsteArbeidsdag(arbeidsdag)
    }

    override fun toString() = "Finn første virkedag etter $dato"

    private tailrec fun finnFørsteArbeidsdag(dato: LocalDate): LocalDate =
        if (dato.arbeidsdag()) {
            dato
        } else {
            finnFørsteArbeidsdag(
                dato.plusDays(1),
            )
        }

    private fun LocalDate.arbeidsdag(): Boolean =
        NorwegianDateUtil.isWorkingDay(Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant()))
}

fun Opplysningstype<LocalDate>.førsteArbeidsdag(dato: Opplysningstype<LocalDate>) = FørsteArbeidsdag(this, dato)
