package no.nav.dagpenger.opplysning

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Collections.unmodifiableMap
import java.util.TreeMap

// Temporal object pattern fra https://martinfowler.com/eaaDev/TemporalObject.html
class TemporalCollection<R> {
    // LocalDateTime er comparable og sorterer naturlig i kronologisk rekkefølge
    private val contents = TreeMap<LocalDateTime, R>()

    fun contents() = unmodifiableMap(contents)

    private val milestones get() = contents.descendingKeySet().toList()

    fun get(date: LocalDateTime): R =
        contents.floorEntry(date)?.value
            ?: throw IllegalArgumentException(
                "No records that early. Asked for date $date. Milestones=$milestones",
            )

    fun get(date: LocalDate): R = get(date.atStartOfDay())

    fun put(
        at: LocalDateTime,
        item: R,
    ) {
        contents[at] = item
    }

    fun put(
        at: LocalDate,
        item: R,
    ) {
        put(at.atStartOfDay(), item)
    }

    /** Eldste → nyeste i kronologisk rekkefølge */
    fun getAll(): List<R> = contents.values.toList()

    override fun toString(): String = "TemporalCollection(contents=$contents)"
}
