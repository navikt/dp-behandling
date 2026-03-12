package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.timer

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
    private val arbeidsdager = arbeidsdager(dager) // todo: Endre til stønadsdager
    private val prosentfaktor = beregnProsentfaktor(dager)

    private val timerArbeidet = dager.mapNotNull { it.timerArbeidet }.summer()
    private val terskel = (100 - terskelstrategi.beregnTerskel(arbeidsdager)) / 100
    val oppfyllerKravTilTaptArbeidstid = (timerArbeidet / sumFva).timer <= terskel
    val resultat = beregnUtbetaling()

    private fun arbeidsdager(dager: Set<Dag>): Set<Arbeidsdag> {
        val arbeidsdager = dager.filterIsInstance<Arbeidsdag>()
        return arbeidsdager.subList(0, minOf(arbeidsdager.size, stønadsdagerIgjen)).toSortedSet()
    }

    private fun beregnProsentfaktor(dager: Set<Dag>): Double {
        val timerArbeidet: Timer = dager.mapNotNull { it.timerArbeidet }.summer()
        return ((sumFva - timerArbeidet) / sumFva).timer
    }

    private fun beregnUtbetaling(): Beregningresultat {
        if (arbeidsdager.isEmpty()) return ingenArbeidsdager
        if (!oppfyllerKravTilTaptArbeidstid) return ingenUtbetaling

        // Grupper arbeidsdager etter dagsats og beregn gradert brutto per gruppe
        val satsgrupper =
            arbeidsdager.groupBy { it.sats }.map { (sats, dager) ->
                val sum = sats * dager.size
                val gradert = sum * prosentfaktor
                SatsGruppe(dager, gradert)
            }

        val totalBrutto = Beløp(satsgrupper.sumOf { it.bruttoBeløp.verdien })

        // Fordel egenandel proporsjonalt og beregn utbetaling per dag
        val forbruksdager =
            satsgrupper
                .flatMap { gruppe ->
                    val egenandelForPeriode = egenandelForPeriode(gruppe.bruttoBeløp, totalBrutto)
                    val nettoBeløp = (gruppe.bruttoBeløp - egenandelForPeriode).avrundetBeløp
                    gruppe.fordelPåDager(nettoBeløp)
                }.sortedBy { it.dag.dato }

        val forbruktEgenandel = Beløp(satsgrupper.sumOf { egenandelForPeriode(it.bruttoBeløp, totalBrutto).verdien })

        return Beregningresultat(
            utbetaling = Beløp(forbruksdager.sumOf { it.tilUtbetaling.verdien }),
            forbruktEgenandel = forbruktEgenandel,
            forbruksdager = forbruksdager,
            gjenståendeEgenandel = gjenståendeEgenandel - forbruktEgenandel,
            oppfyllerKravTilTaptArbeidstid = true,
            sumFva = sumFva,
            sumArbeidstimer = timerArbeidet,
            prosentfaktor = prosentfaktor,
        )
    }

    /** Beregner gruppens proporsjonale andel av egenandelen basert på gruppens andel av total brutto. */
    private fun egenandelForPeriode(
        gruppeBrutto: Beløp,
        totalBrutto: Beløp,
    ): Beløp {
        if (totalBrutto == Beløp(0.0)) return Beløp(0)
        val andel = (gruppeBrutto / totalBrutto).verdien
        val beregnetEgenandel = Beløp(gjenståendeEgenandel.verdien * andel).avrundetBeløp
        return minOf(gruppeBrutto, beregnetEgenandel)
    }

    private val ingenArbeidsdager
        get() =
            Beregningresultat(
                utbetaling = Beløp(verdi = 0),
                forbruktEgenandel = Beløp(0),
                forbruksdager = emptyList(),
                gjenståendeEgenandel = gjenståendeEgenandel,
                oppfyllerKravTilTaptArbeidstid = true,
                sumFva = 0.0.timer,
                sumArbeidstimer = 0.0.timer,
                prosentfaktor = 0.0,
            )

    private val ingenUtbetaling
        get() =
            Beregningresultat(
                utbetaling = Beløp(verdi = 0),
                forbruktEgenandel = Beløp(0),
                forbruksdager = emptyList(),
                gjenståendeEgenandel = gjenståendeEgenandel,
                oppfyllerKravTilTaptArbeidstid = false,
                sumFva = sumFva,
                sumArbeidstimer = timerArbeidet,
                prosentfaktor = prosentfaktor,
            )

    internal fun interface Terskelstrategi {
        fun beregnTerskel(dager: Set<Arbeidsdag>): Double
    }

    companion object {
        private val snitterskel: Terskelstrategi = Terskelstrategi { it.sumOf { arbeidsdag -> arbeidsdag.terskel }.toDouble() / it.size }
    }
}

data class Beregningresultat(
    val utbetaling: Beløp,
    val forbruktEgenandel: Beløp,
    val forbruksdager: List<Forbruksdag>,
    val gjenståendeEgenandel: Beløp,
    val oppfyllerKravTilTaptArbeidstid: Boolean,
    val sumFva: Timer,
    val sumArbeidstimer: Timer,
    val prosentfaktor: Double,
) {
    data class Forbruksdag(
        val dag: Dag,
        val tilUtbetaling: Beløp,
    )
}

/** Arbeidsdager med lik dagsats, med beregnet brutto utbetaling (sats × antall dager × prosentfaktor). */
private class SatsGruppe(
    val arbeidsdager: List<Arbeidsdag>,
    val bruttoBeløp: Beløp,
) {
    /** Fordeler et beløp jevnt på arbeidsdager, med eventuell øre-rest på siste dag. */
    fun fordelPåDager(beløp: Beløp): List<Beregningresultat.Forbruksdag> {
        if (arbeidsdager.isEmpty()) return emptyList()
        val antall = arbeidsdager.size.toBigDecimal()
        val rest = Beløp(beløp.verdien % antall)
        val dagsbeløp = (beløp - rest) / Beløp(antall)
        return arbeidsdager.mapIndexed { index, dag ->
            val erSisteDag = index == arbeidsdager.lastIndex
            val tilUtbetaling = if (erSisteDag) dagsbeløp + rest else dagsbeløp
            Beregningresultat.Forbruksdag(dag, tilUtbetaling)
        }
    }
}
