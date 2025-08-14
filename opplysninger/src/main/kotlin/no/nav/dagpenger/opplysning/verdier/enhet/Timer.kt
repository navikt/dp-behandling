package no.nav.dagpenger.opplysning.verdier.enhet

import kotlin.time.Duration

@JvmInline
value class Timer(
    val timer: Double,
) : Comparable<Timer> {
    constructor(number: Number) : this(number.toDouble())

    operator fun div(number: Int): Timer = Timer(this.timer / number)

    operator fun div(number: Timer): Timer = Timer(this.timer / number.timer)

    operator fun minus(other: Timer) = Timer(this.timer - other.timer)

    operator fun plus(other: Timer) = Timer(this.timer + other.timer)

    operator fun times(other: Double): Timer = Timer(this.timer * other)

    override fun compareTo(other: Timer): Int = this.timer.compareTo(other.timer)

    override fun toString() = "Timer($timer)"

    companion object {
        val Number.timer get() = Timer(this)
        val Duration.tilTimer: Timer get() = (this.inWholeMinutes.toDouble() / 60).timer

        fun List<Timer>.summer() = this.sumOf { it.timer }.timer
    }
}
