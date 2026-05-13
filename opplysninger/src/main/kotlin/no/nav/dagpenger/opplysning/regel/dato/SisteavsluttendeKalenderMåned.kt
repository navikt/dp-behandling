package no.nav.dagpenger.opplysning.regel.dato

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate

/***
 * For å finne siste avsluttende kalendermåned for en dato gitt en rapporteringsfrist og
 *
 */

class SisteavsluttendeKalenderMåned internal constructor(
    produserer: Opplysningstype<LocalDate>,
    private val dato: Opplysningstype<LocalDate>,
    private val terskeldato: Opplysningstype<LocalDate>,
) : Regel<LocalDate>(produserer, listOf(dato, terskeldato)) {
    override fun kjør(
        opplysninger: LesbarOpplysninger,
        prøvingsdato: LocalDate,
    ): LocalDate {
        val a = opplysninger.finnOpplysning(dato).verdi
        val b = opplysninger.finnOpplysning(terskeldato).verdi
        val månederTilbake =
            when (a.førEllerLik(b)) {
                true -> 2
                false -> 1
            }

        val avsluttendeKalenderMåned = b.minusMonths(månederTilbake.toLong())
        return avsluttendeKalenderMåned.withDayOfMonth(avsluttendeKalenderMåned.lengthOfMonth())
    }

    override fun toString() = "Fastsetter siste avsluttende kalendermåned hvor $dato er etter $terskeldato"
}

private fun LocalDate.førEllerLik(dato: LocalDate) = this.isBefore(dato) || this.isEqual(dato)

fun Opplysningstype<LocalDate>.sisteAvsluttendeKalenderMåned(
    søknadstidspunkt: Opplysningstype<LocalDate>,
    rapporteringsfrist: Opplysningstype<LocalDate>,
) = SisteavsluttendeKalenderMåned(this, søknadstidspunkt, rapporteringsfrist)
