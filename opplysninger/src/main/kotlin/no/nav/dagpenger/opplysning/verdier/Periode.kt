package no.nav.dagpenger.opplysning.verdier

import java.time.LocalDate

data class Periode(
    override val start: LocalDate,
    override val endInclusive: LocalDate,
) : ClosedRange<LocalDate>,
    Comparable<Periode>,
    Iterable<LocalDate> {
    val fraOgMed = start
    val tilOgMed = endInclusive

    override fun compareTo(other: Periode): Int = start.compareTo(other.start) + endInclusive.compareTo(other.endInclusive)

    override fun iterator(): Iterator<LocalDate> =
        object : Iterator<LocalDate> {
            private var current = start

            override fun hasNext() = current <= endInclusive

            override fun next(): LocalDate = current.apply { current = current.plusDays(1) }
        }
}
