package no.nav.dagpenger.regel.beregning

import java.math.BigDecimal
import java.time.LocalDate

internal sealed interface Dag {
    val dato: LocalDate
    val sats: Int?
    val fva: Double?
    val timerArbeidet: Int?
}

internal class Arbeidsdag(
    override val dato: LocalDate,
    override val sats: Int,
    override val fva: Double,
    override val timerArbeidet: Int,
    val terskel: BigDecimal,
) : Dag {
    var forbruktEgenandel: Double = 0.0
        private set
    var dagsbeløp: Double = 0.0
        internal set
    val tilUtbetaling get() = dagsbeløp - forbruktEgenandel

    fun forbrukEgenandel(egenandel: Double) {
        forbruktEgenandel = minOf(egenandel, dagsbeløp)
    }

    constructor(dato: LocalDate, sats: Int, fva: Double, timerArbeidet: Int, terskel: Double) :
        this(dato, sats, fva, timerArbeidet, BigDecimal.valueOf(terskel))
}

internal class Fraværsdag(
    override val dato: LocalDate,
) : Dag {
    override val sats = null
    override val fva = null
    override val timerArbeidet = null
}

internal class Helgedag(
    override val dato: LocalDate,
    override val timerArbeidet: Int?,
) : Dag {
    override val sats = null
    override val fva = null
}
