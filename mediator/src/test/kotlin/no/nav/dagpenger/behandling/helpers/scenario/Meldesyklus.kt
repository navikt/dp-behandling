package no.nav.dagpenger.behandling.helpers.scenario

import no.nav.dagpenger.opplysning.verdier.Periode
import java.time.LocalDate

class Meldesyklus(
    søknadsdato: LocalDate,
) {
    // Finn første mandag før søknadsdato
    private val førsteMandag = søknadsdato.minusDays(((søknadsdato.dayOfWeek.value + 6) % 7).toLong())

    fun periode(nummer: Int): Periode {
        val førsteDag = førsteMandag.plusWeeks((nummer - 1).toLong() * 2)
        return Periode(førsteDag, førsteDag.plusDays(13)).also {
            ""
            println("Meldeperiode $nummer: ${it.fraOgMed} - ${it.tilOgMed}")
        }
    }
}
