package no.nav.dagpenger.opplysning

import java.time.LocalDate
import java.time.LocalDate.MAX
import java.time.LocalDate.MIN

data class PeriodisertVerdi<T>(
    val fraOgMed: LocalDate,
    var tilOgMed: LocalDate = MAX,
    val verdi: T,
)

typealias Tidslinje<T> = List<PeriodisertVerdi<T>>

class TidslinjeBygger<T : Comparable<T>>(
    private val opplysninger: Collection<Opplysning<T>>,
) {
    val sortertOpplysninger = opplysninger.sortedBy { it.gyldighetsperiode.fraOgMed }

    fun medLikVerdi(default: T? = null) = lagPeriode { opplysninger -> opplysninger.singleOrNull()?.verdi ?: default }

    fun lagPeriode(evaluerVerdi: (Collection<Opplysning<T>>) -> T?): Tidslinje<T> {
        if (opplysninger.isEmpty()) return emptyList()

        val perioder: Sequence<PeriodisertVerdi<T>> =
            skjæringsdatoer(opplysninger).zipWithNext().mapNotNull { (start, slutt) ->
                val sluttdato = slutt.takeIf { !it.isEqual(MAX) }?.minusDays(1) ?: MAX

                val verdi = evaluerVerdi(verdierPåSkjæringsdato(start))
                if (verdi == null) return@mapNotNull null
                PeriodisertVerdi(start, sluttdato, verdi)
            }

        // Slå sammen perioder som ligger kant i kant
        return slåSammenLike(perioder)
    }

    // Dette er en flaskehals, men alle optimaliseringer gjør koden fryktelig mye mindre lesbar
    private fun verdierPåSkjæringsdato(start: LocalDate): List<Opplysning<T>> =
        sortertOpplysninger.filter { it.gyldighetsperiode.inneholder(start) }

    private fun skjæringsdatoer(utfall: Collection<Opplysning<*>>): Sequence<LocalDate> =
        utfall
            .asSequence()
            .flatMap { sequenceOf(it.gyldighetsperiode.fraOgMed, it.gyldighetsperiode.tilOgMed.nesteDag()) }
            .distinct()
            .filterNot { it.isEqual(MIN) } // Fjerner MIN som er default verdi
            .sorted()

    // Legg til en dag for emulere endExclusive som gjør zipWithNext enklere
    private fun LocalDate.nesteDag(): LocalDate = if (this.isEqual(MAX)) this else this.plusDays(1)

    private fun slåSammenLike(perioder: Sequence<PeriodisertVerdi<T>>): MutableList<PeriodisertVerdi<T>> {
        val resultat = mutableListOf<PeriodisertVerdi<T>>()

        for (neste in perioder) {
            val siste = resultat.lastOrNull()

            if (siste != null && siste.verdi == neste.verdi && siste.tilOgMed.nesteDag().isEqual(neste.fraOgMed)) {
                resultat[resultat.lastIndex] = siste.copy(tilOgMed = neste.tilOgMed)
            } else {
                resultat.add(neste)
            }
        }

        return resultat
    }

    companion object {
        fun hvorAlleVilkårErOppfylt(): (Collection<Opplysning<Boolean>>) -> Boolean? =
            { påDato -> påDato.isNotEmpty() && påDato.all { it.verdi } }
    }
}
