package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer

class Beregningsperiode private constructor(
    private val gjenståendeEgenandel: Beløp,
    dager: Set<Dag>,
    terskelstrategi: Terskelstrategi,
    private val stønadsdagerIgjen: Int,
) {
    constructor(gjenståendeEgenandel: Beløp, dag: Set<Dag>, stønadsdagerIgjen: Int) : this(
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

    private val sumFva = dager.mapNotNull { it.fva }.summer()
    private val arbeidsdager = arbeidsdager(dager)
    private val prosentfaktor = beregnProsentfaktor(dager)
    private val timerArbeidet = dager.mapNotNull { it.timerArbeidet }.summer()
    val terskel = (100 - terskelstrategi.beregnTerskel(arbeidsdager)) / 100
    val oppfyllerKravTilTaptArbeidstid = (timerArbeidet / sumFva).timer <= terskel

    val utbetaling = beregnUtbetaling(arbeidsdager)

    // TODO: Forbruksdager må filtreres ytterligere for å ta hensyn til _faktisk_ forbruk (ekempelvis ved sanksjonsdager)
    val forbruksdager = if (oppfyllerKravTilTaptArbeidstid) arbeidsdager.toList() else emptyList()

    private fun arbeidsdager(dager: Set<Dag>): Set<Arbeidsdag> {
        val arbeidsdager = dager.filterIsInstance<Arbeidsdag>()
        return arbeidsdager.subList(0, minOf(arbeidsdager.size, stønadsdagerIgjen)).toSortedSet()
    }

    private fun beregnProsentfaktor(dager: Set<Dag>): Timer {
        val timerArbeidet: Timer = dager.mapNotNull { it.timerArbeidet }.summer()
        return (sumFva - timerArbeidet) / sumFva
    }

    private fun beregnUtbetaling(arbeidsdager: Set<Arbeidsdag>): Int {
        val fordeling: List<Arbeidsdag> = beregnDagsløp(arbeidsdager).sorted()
        val trekkEgenandel: List<Arbeidsdag> = fordelEgenandel(fordeling)

        val overskytendeRest = Beløp(trekkEgenandel.sumOf { it.uavrundetUtbetaling.verdien % 1.toBigDecimal() })
        val overskytendeEgenandel = Beløp(trekkEgenandel.sumOf { it.uavRundetforbruktEgenandel.verdien % 1.toBigDecimal() }).avrundetBeløp

        trekkEgenandel.lastOrNull()?.overskytendeRest(overskytendeRest)
        trekkEgenandel.lastOrNull()?.overskytendeEgenandel(overskytendeEgenandel)

        return trekkEgenandel.sumOf(Arbeidsdag::avrundetUtbetaling)
    }

    private fun beregnDagsløp(arbeidsdager: Set<Arbeidsdag>): Set<Arbeidsdag> =
        arbeidsdager.onEach { it.dagsbeløp = it.sats * prosentfaktor }

    private fun fordelEgenandel(fordeling: List<Arbeidsdag>): List<Arbeidsdag> {
        val totalTilUtbetaling = Beløp(fordeling.sumOf { it.dagsbeløp.verdien })
        if (totalTilUtbetaling == Beløp(0.0)) return fordeling
        return fordeling.onEach {
            val egenandelPerDag = minOf(it.dagsbeløp, (it.dagsbeløp / totalTilUtbetaling) * gjenståendeEgenandel)
            it.forbrukEgenandel(egenandelPerDag)
        }
    }

    internal fun interface Terskelstrategi {
        fun beregnTerskel(dager: Set<Arbeidsdag>): Double
    }

    companion object {
        private val snitterskel: Terskelstrategi =
            Terskelstrategi { it.sumOf { arbeidsdag -> arbeidsdag.terskel }.toDouble() / it.size }
    }
}
