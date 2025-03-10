package no.nav.dagpenger.opplysning.verdier

import java.time.LocalDate

data class Periode(
    override val start: LocalDate,
    override val endInclusive: LocalDate,
) : ClosedRange<LocalDate>,
    Comparable<Periode> {
    val fraOgMed = start
    val tilOgMed = endInclusive

    override fun compareTo(other: Periode): Int = start.compareTo(other.start) + endInclusive.compareTo(other.endInclusive)
}
