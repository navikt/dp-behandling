package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import java.math.BigDecimal
import java.time.LocalDate

internal sealed interface Dag {
    val dato: LocalDate
    val sats: Beløp?
    val fva: Timer?
    val timerArbeidet: Timer?
}

internal class Arbeidsdag(
    override val dato: LocalDate,
    override val sats: Beløp,
    override val fva: Timer,
    override val timerArbeidet: Timer,
    val terskel: BigDecimal,
) : Dag {
    internal var overskytendeRest: Beløp = Beløp(0.0)
        private set
    internal var forbruktEgenandel: Beløp = Beløp(0.0)
        private set
    var dagsbeløp: Beløp = Beløp(0.0)
        internal set
    internal val uavrundetUtbetaling get() = dagsbeløp - forbruktEgenandel

    val avrundetUtbetaling: Int get() = uavrundetUtbetaling.avrundetNedover.toInt() + overskytendeRest.avrundet.toInt()

    fun forbrukEgenandel(egenandel: Beløp) {
        forbruktEgenandel = minOf(egenandel, dagsbeløp)
    }

    fun overskytendeRest(overskytende: Beløp) {
        overskytendeRest = overskytende
    }

    constructor(dato: LocalDate, sats: Beløp, fva: Timer, timerArbeidet: Timer, terskel: Double) :
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
    override val timerArbeidet: Timer?,
) : Dag {
    override val sats = null
    override val fva = null
}
