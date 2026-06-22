package no.nav.dagpenger.regel.regelsett.beregning

import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.timer

class Beregningsperiode private constructor(
    private val gjenståendeEgenandel: Beløp,
    private val meldedager: Set<Dag>,
    terskelstrategi: Terskelstrategi,
    private val stønadsdagerIgjen: Int,
    private val bortfallsdagerIgjen: Int,
) {
    constructor(gjenståendeEgenandel: Beløp, dag: Set<Dag>, stønadsdagerIgjen: Int, bortfallsdagerIgjen: Int = 0) : this(
        gjenståendeEgenandel,
        dag,
        snitterskel,
        stønadsdagerIgjen,
        bortfallsdagerIgjen,
    )

    init {
        require(meldedager.size <= 14) { "En beregningsperiode kan maksimalt inneholde 14 dager" }
    }

    /***
     * Beregning av ett MK, Udos naive forbruksteller:
     * ·       Vi har funnet at det er «rettighetsdager (dager fom. Virkningstidspunkt) i meldeperioden, og hvilke dager dette er.
     * ·       Begynnende med tidligste dag spør vi : er den dagen vurdert tidligere med utfall ja?
     * ·       Dersom «ja»: vurderes ikke dagen, med mindre det skal kjøres  en revurdering
     * ·       Dersom  «nei»: spør vi: er det tidligere dager i rettighetsperioden som er vurdert med utfall ja?
     * ·       Dersom «ja» finner vi den dagen og denne har opplysning om hvor mange dager med rettighet som gjenstår etter.
     */

    private val sumFva = meldedager.mapNotNull { it.fva }.summer()
    private val arbeidsdager = arbeidsdager(meldedager) // todo: Endre til stønadsdager
    private val prosentfaktor = beregnProsentfaktor(meldedager)

    private val timerArbeidet = meldedager.mapNotNull { it.timerArbeidet }.summer()
    private val terskel = (100 - terskelstrategi.beregnTerskel(arbeidsdager)) / 100
    val oppfyllerKravTilTaptArbeidstid = (timerArbeidet / sumFva).timer <= terskel
    val resultat = beregnUtbetaling()

    private fun arbeidsdager(dager: Set<Dag>): Set<Arbeidsdag> {
        val arbeidsdager = dager.filterIsInstance<Arbeidsdag>()
        return arbeidsdager.subList(0, minOf(arbeidsdager.size, stønadsdagerIgjen)).toSortedSet()
    }

    private fun beregnProsentfaktor(dager: Set<Dag>): Double {
        val timerArbeidet: Timer = dager.mapNotNull { it.timerArbeidet }.summer()
        return maxOf(0.0, ((sumFva - timerArbeidet) / sumFva).timer)
    }

    private fun beregnUtbetaling(): Beregningresultat {
        if (arbeidsdager.isEmpty()) return ingenArbeidsdager
        if (!oppfyllerKravTilTaptArbeidstid) return ingenUtbetaling

        // Skill bortfallsdager fra utbetalingsdager — tidligste dager først er bortfall
        val sortert = arbeidsdager.sorted()
        val bortfallsdager = sortert.take(bortfallsdagerIgjen).toSet()
        val utbetalingsdager = sortert.drop(bortfallsdagerIgjen).toSet()

        // Bortfallsdager: forbruk men 0 utbetaling, ingen egenandel
        val bortfallForbruksdager =
            bortfallsdager.map {
                Beregningresultat.Beregningsdag.Forbruksdag(
                    dag = it,
                    tilUtbetaling = Beløp(0),
                    erBortfall = true,
                )
            }

        // Beregn utbetaling og egenandel kun for ikke-bortfallsdager
        if (utbetalingsdager.isEmpty()) {
            return Beregningresultat(
                utbetaling = Beløp(0),
                forbruktEgenandel = Beløp(0),
                forbruksdager = bortfallForbruksdager.sortedBy { it.dag.dato },
                meldedager = meldedager,
                gjenståendeEgenandel = gjenståendeEgenandel,
                oppfyllerKravTilTaptArbeidstid = true,
                sumFva = sumFva,
                sumArbeidstimer = timerArbeidet,
                prosentfaktor = prosentfaktor,
            )
        }

        val satsgrupper =
            utbetalingsdager.groupBy { it.sats }.map { (sats, dager) ->
                val sum = sats * dager.size
                val gradert = sum * prosentfaktor
                SatsGruppe(dager, gradert)
            }

        val totalBrutto = Beløp(satsgrupper.sumOf { it.bruttoBeløp.verdien })
        val grupperMedEgenandel = satsgrupper.map { it to egenandelForPeriode(it.bruttoBeløp, totalBrutto) }

        val utbetalingsForbruksdager =
            grupperMedEgenandel.flatMap { (gruppe, egenandelForGruppe) ->
                val netto = (gruppe.bruttoBeløp - egenandelForGruppe).avrundetBeløp
                gruppe.fordelPåDager(netto)
            }

        val forbruktEgenandel = Beløp(grupperMedEgenandel.sumOf { (_, egenandel) -> egenandel.verdien })
        val alleForbruksdager = (bortfallForbruksdager + utbetalingsForbruksdager).sortedBy { it.dag.dato }

        return Beregningresultat(
            utbetaling = Beløp(alleForbruksdager.sumOf { it.tilUtbetaling.verdien }),
            forbruktEgenandel = forbruktEgenandel,
            forbruksdager = alleForbruksdager,
            meldedager = meldedager,
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
                meldedager = meldedager,
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
                meldedager = meldedager,
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
    internal val forbruksdager: List<Beregningsdag.Forbruksdag>,
    private val meldedager: Set<Dag>,
    val gjenståendeEgenandel: Beløp,
    val oppfyllerKravTilTaptArbeidstid: Boolean,
    val sumFva: Timer,
    val sumArbeidstimer: Timer,
    val prosentfaktor: Double,
) {
    val beregningsdager: List<Beregningsdag>
        get() {
            val forbruktDager = forbruksdager.map { it.dag.dato to it }.toMap()
            return meldedager.map { dag ->
                forbruktDager[dag.dato] ?: Beregningsdag.IkkeForbruksdag(dag)
            }
        }

    sealed interface Beregningsdag {
        val dag: Dag
        val tilUtbetaling: Beløp
        val gyldighetsperiode get() = Gyldighetsperiode.kun(dag.dato)
        val erBortfall: Boolean get() = false

        data class Forbruksdag(
            override val dag: Dag,
            override val tilUtbetaling: Beløp,
            override val erBortfall: Boolean = false,
        ) : Beregningsdag

        data class IkkeForbruksdag(
            override val dag: Dag,
        ) : Beregningsdag {
            override val tilUtbetaling: Beløp = Beløp(0.0)
        }
    }
}

/** Arbeidsdager med lik dagsats, med beregnet brutto utbetaling (sats × antall dager × prosentfaktor). */
private class SatsGruppe(
    val arbeidsdager: List<Arbeidsdag>,
    val bruttoBeløp: Beløp,
) {
    /** Fordeler et beløp jevnt på arbeidsdager, med eventuell øre-rest på siste dag. */
    fun fordelPåDager(beløp: Beløp): List<Beregningresultat.Beregningsdag.Forbruksdag> {
        if (arbeidsdager.isEmpty()) return emptyList()
        val antall = arbeidsdager.size.toBigDecimal()
        val rest = Beløp(beløp.verdien % antall)
        val dagsbeløp = (beløp - rest) / Beløp(antall)
        return arbeidsdager.mapIndexed { index, dag ->
            val erSisteDag = index == arbeidsdager.lastIndex
            Beregningresultat.Beregningsdag.Forbruksdag(
                dag = dag,
                tilUtbetaling = if (erSisteDag) dagsbeløp + rest else dagsbeløp,
            )
        }
    }
}
