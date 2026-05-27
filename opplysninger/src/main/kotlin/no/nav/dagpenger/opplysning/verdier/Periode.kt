package no.nav.dagpenger.opplysning.verdier

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Periode(
    override val start: LocalDate,
    override val endInclusive: LocalDate,
) : ClosedRange<LocalDate>,
    Comparable<Periode>,
    Iterable<LocalDate> {
    constructor(dag: LocalDate) : this(dag, dag)

    val fraOgMed = start
    val tilOgMed = endInclusive

    override fun compareTo(other: Periode): Int = start.compareTo(other.start) + endInclusive.compareTo(other.endInclusive)

    override fun iterator(): Iterator<LocalDate> =
        object : Iterator<LocalDate> {
            private var current = start

            override fun hasNext() = current <= endInclusive

            override fun next(): LocalDate = current.apply { current = current.plusDays(1) }
        }

    override fun toString() = "${start.format(datoFormatterer)} til ${endInclusive.format(datoFormatterer)}"

    private companion object {
        val datoFormatterer = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
