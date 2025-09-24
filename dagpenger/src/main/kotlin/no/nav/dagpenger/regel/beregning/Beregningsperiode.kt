package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer

data class Bøtte(
    val arbeidsdager: List<Arbeidsdag>,
    val egenandel: Beløp,
    val utbetalt: Beløp,
    val reminder: Beløp = Beløp(utbetalt.verdien % arbeidsdager.size.toBigDecimal()),
    val dagsbeløp: Int = ((utbetalt - reminder) / arbeidsdager.size).heleKroner.toInt(),
    val beløpSisteDag: Int = dagsbeløp + reminder.heleKroner.toInt(),
)

data class Beregningresultat(
    val utbetaling: Int,
    val forbruktEgenandel: Int,
    val forbruksdager: List<Forbruksdag>,
    val gjenståendeEgenandel: Beløp,
    val oppfyllerKravTilTaptArbeidstid: Boolean,
) {
    data class Forbruksdag(
        val dag: Dag,
        val tilUtbetaling: Int,
    )
}

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

    private fun beregnProsentfaktor(dager: Set<Dag>): Timer {
        val timerArbeidet: Timer = dager.mapNotNull { it.timerArbeidet }.summer()
        return (sumFva - timerArbeidet) / sumFva
    }

    private fun beregnUtbetaling(): Beregningresultat {
        if (!oppfyllerKravTilTaptArbeidstid) {
            return Beregningresultat(0, 0, emptyList(), gjenståendeEgenandel, false)
        }

        val dagerGruppertPåSats = arbeidsdager.groupBy { it.sats }
        val dagerGruppertPåSatsGradert =
            dagerGruppertPåSats.map { (sats, dager) ->
                ((sats * dager.size) * prosentfaktor) to dager
            }

        val sumFørEgenAndelstrekk = Beløp(dagerGruppertPåSatsGradert.sumOf { it.first.verdien })

        val bøtter =
            dagerGruppertPåSatsGradert.map { (bøtteSum, arbeidsdager) ->
                val bøtteStørrelseIProsent =
                    if (sumFørEgenAndelstrekk == Beløp(0.0)) {
                        0.0.toBigDecimal()
                    } else {
                        (bøtteSum / sumFørEgenAndelstrekk).verdien
                    }
                val egenandel =
                    minOf(
                        sumFørEgenAndelstrekk,
                        Beløp(gjenståendeEgenandel.verdien * bøtteStørrelseIProsent).avrundetBeløp,
                    )
                Bøtte(
                    arbeidsdager = arbeidsdager,
                    egenandel = egenandel,
                    utbetalt = (bøtteSum - egenandel).avrundetBeløp,
                )
            }

        val forbruksdager =
            bøtter
                .flatMap { bøtte ->
                    bøtte.arbeidsdager.map { dag ->
                        val tilUtbetaling =
                            if (dag.dato.isEqual(bøtte.arbeidsdager.last().dato)) {
                                bøtte.beløpSisteDag
                            } else {
                                bøtte.dagsbeløp
                            }
                        Beregningresultat.Forbruksdag(dag, tilUtbetaling)
                    }
                }.sortedBy { it.dag.dato }

        val forbruktEgenandel = Beløp(bøtter.sumOf { it.egenandel.verdien })
        return Beregningresultat(
            utbetaling = bøtter.sumOf { it.utbetalt.verdien }.intValueExact(),
            forbruktEgenandel = forbruktEgenandel.heleKroner.toInt(),
            forbruksdager = forbruksdager,
            gjenståendeEgenandel = gjenståendeEgenandel - forbruktEgenandel,
            oppfyllerKravTilTaptArbeidstid = true,
        )
    }

    internal fun interface Terskelstrategi {
        fun beregnTerskel(dager: Set<Arbeidsdag>): Double
    }

    companion object {
        private val snitterskel: Terskelstrategi =
            Terskelstrategi { it.sumOf { arbeidsdag -> arbeidsdag.terskel }.toDouble() / it.size }
    }
}
