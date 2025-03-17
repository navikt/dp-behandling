package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.regel.beregning.Beregningsperiode.Terskelstrategi

internal class Beregningsperiode private constructor(
    private val gjenståendeEgenandel: Double,
    dager: List<Dag>,
    terskelstrategi: Terskelstrategi,
) {
    constructor(gjenståendeEgenandel: Double, dag: List<Dag>) : this(gjenståendeEgenandel, dag, snitterskel)

    init {
        require(dager.size <= 14) { "En beregningsperiode kan maksimalt inneholde 14 dager" }
    }

    private val sumFva = dager.mapNotNull { it.fva }.sum()
    private val arbeidsdager = dager.filterIsInstance<Arbeidsdag>()
    private val prosentfaktor = beregnProsentfaktor(dager)
    val terskel = terskelstrategi.beregnTerskel(arbeidsdager) / 100
    val oppfyllerKravTilTaptArbeidstid = (arbeidsdager.sumOf { it.timerArbeidet } / sumFva) <= terskel

    val utbetaling = beregnUtbetaling(arbeidsdager)

    val forbruksdager = if (oppfyllerKravTilTaptArbeidstid) arbeidsdager else emptyList()

    private fun beregnProsentfaktor(dager: List<Dag>): Double {
        val timerArbeidet = dager.mapNotNull { it.timerArbeidet }.sum()
        return (sumFva - timerArbeidet) / sumFva
    }

    private fun beregnUtbetaling(arbeidsdager: List<Arbeidsdag>): Double {
        val fordeling = fordelBeløpPåDager(arbeidsdager)
        val trekkEgenandel = fordelEgenandel(fordeling)
        return trekkEgenandel.sumOf(Arbeidsdag::tilUtbetaling)
    }

    private fun fordelBeløpPåDager(arbeidsdager: List<Arbeidsdag>): List<Arbeidsdag> =
        arbeidsdager
            .groupBy { it.sats }
            .flatMap { (sats, dagerMedDenneSatsen) ->
                val sumForPeriode = (sats * dagerMedDenneSatsen.size) * prosentfaktor
                val sumForDag = sumForPeriode / dagerMedDenneSatsen.size
                dagerMedDenneSatsen.onEach { it.dagsbeløp = sumForDag }
            }

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
