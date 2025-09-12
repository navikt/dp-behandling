package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import java.math.BigDecimal
import java.time.LocalDate

sealed interface Dag : Comparable<Dag> {
    val dato: LocalDate
    val sats: Beløp?
    val fva: Timer?
    val timerArbeidet: Timer?

    override fun compareTo(other: Dag) = dato.compareTo(other.dato)
}

class Arbeidsdag(
    override val dato: LocalDate,
    override val sats: Beløp,
    override val fva: Timer,
    override val timerArbeidet: Timer,
    val terskel: BigDecimal,
) : Dag {
    var dagsbeløp: Beløp = Beløp(0.0)
        internal set

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
