package no.nav.dagpenger.regel.beregning

import java.math.BigDecimal
import java.math.RoundingMode

internal class Beregningsperiode private constructor(
    private val gjenståendeEgenandel: Double,
    dager: List<Dag>,
    terskelstrategi: Terskelstrategi,
    private val stønadsdagerIgjen: Int,
) {
    constructor(gjenståendeEgenandel: Double, dag: List<Dag>, stønadsdagerIgjen: Int) : this(
        gjenståendeEgenandel,
        dag,
        snitterskel,
        stønadsdagerIgjen,
    )

    init {
        require(dager.size <= 14) { "En beregningsperiode kan maksimalt inneholde 14 dager" }
    }

    /***
     * Beregning av ett MK, Udos naive forbruksteller:
     * ·       Vi har funnet at det er «rettighetsdager (dager fom. Virkningstidspunkt) i meldeperioden, og hvilke dager dette er.
     * ·       Begynnende med tidligste dag spør vi : er den dagen vurdert tidligere med utfall ja?
     * ·       Dersom «ja»: vurderes ikke dagen, med mindre det skal kjøres  en revurdering
     * ·       Dersom  «nei»: spør vi: er det tidligere dager i rettighetsperioden som er vurdert med utfall ja?
     * ·       Dersom «ja» finner vi den dagen og denne har opplysning om hvor mange dager med rettighet som gjenstår etter.
     */

    private val sumFva = dager.mapNotNull { it.fva }.sum()
    private val arbeidsdager = arbeidsdager(dager)
    private val prosentfaktor = beregnProsentfaktor(dager)
    val terskel = (100 - terskelstrategi.beregnTerskel(arbeidsdager)) / 100
    val oppfyllerKravTilTaptArbeidstid = (arbeidsdager.sumOf { it.timerArbeidet } / sumFva) <= terskel

    val utbetaling = beregnUtbetaling(arbeidsdager)

    val forbruksdager = if (oppfyllerKravTilTaptArbeidstid) arbeidsdager else emptyList()

    private fun arbeidsdager(dager: List<Dag>): List<Arbeidsdag> {
        val arbeidsdager = dager.filterIsInstance<Arbeidsdag>()
        return arbeidsdager.subList(0, minOf(arbeidsdager.size, stønadsdagerIgjen))
    }

    private fun beregnProsentfaktor(dager: List<Dag>): Double {
        val timerArbeidet = dager.mapNotNull { it.timerArbeidet }.sum()
        return (sumFva - timerArbeidet) / sumFva
    }

    private fun beregnUtbetaling(arbeidsdager: List<Arbeidsdag>): Int {
        val fordeling: List<Arbeidsdag> = beregnDagsløp(arbeidsdager)
        val trekkEgenandel: List<Arbeidsdag> = fordelEgenandel(fordeling)
        // Avrunde

        val decimalReminder =
            trekkEgenandel.fold(0.0) { acc, arbeidsdag -> acc + arbeidsdag.tilUtbetaling % 1 }

        val avrundet =
            trekkEgenandel.onEach { arbeidsdag ->
                val avrundetBeløp = BigDecimal.valueOf(arbeidsdag.tilUtbetaling).setScale(0, RoundingMode.FLOOR).toInt()
                arbeidsdag.avrundetTilUtbetaling = avrundetBeløp
            }

        val rest = BigDecimal.valueOf(decimalReminder).setScale(0, RoundingMode.HALF_UP).toInt()
        avrundet.lastOrNull()?.let {
            it.avrundetTilUtbetaling = it.avrundetTilUtbetaling + rest
        }
        return avrundet.sumOf(Arbeidsdag::avrundetTilUtbetaling)
    }

    private fun beregnDagsløp(arbeidsdager: List<Arbeidsdag>): List<Arbeidsdag> =
        arbeidsdager.onEach { it.dagsbeløp = it.sats * prosentfaktor }

    private fun fordelEgenandel(fordeling: List<Arbeidsdag>): List<Arbeidsdag> {
        val totalTilUtbetaling = fordeling.sumOf { it.dagsbeløp }
        return fordeling.onEach {
            val egenandelPerDag = minOf(it.dagsbeløp, it.dagsbeløp / totalTilUtbetaling * gjenståendeEgenandel)
            it.forbrukEgenandel(egenandelPerDag)
        }
    }

    internal fun interface Terskelstrategi {
        fun beregnTerskel(dager: List<Arbeidsdag>): Double
    }

    companion object {
        private val snitterskel: Terskelstrategi =
            Terskelstrategi { it.sumOf { arbeidsdag -> arbeidsdag.terskel }.toDouble() / it.size }
    }
}
