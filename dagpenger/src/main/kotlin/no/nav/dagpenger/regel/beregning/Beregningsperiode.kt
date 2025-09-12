package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer
import java.math.BigDecimal

data class Bøtte(
    val sats: Beløp,
    val arbeidsdager: List<Arbeidsdag>,
    val totalBeløp: Beløp,
    val egenandelProsent: BigDecimal,
    val egenandel: Beløp,
    val utbetalt: Beløp,
    val avrundetBeløpPerDag: Int = (utbetalt / arbeidsdager.size).avrundetNedoverBeløp.verdien.toInt(),
    val reminder: Beløp = Beløp(utbetalt.verdien % arbeidsdager.size.toBigDecimal()),
    val dagsbeløp: Int = ((utbetalt - reminder) / arbeidsdager.size).heleKroner.toInt(),
    val beløpSisteDag: Int = dagsbeløp + reminder.heleKroner.toInt(),
    val totaltUtbetalt: Int = avrundetBeløpPerDag * arbeidsdager.size,
    val egenandelPerDag: Beløp = egenandel / arbeidsdager.size,
    val rest: Beløp = utbetalt - Beløp(totaltUtbetalt),
)

data class Beregningresultat(
    val utbetaling: Int,
    val forbruktEgenandel: Int,
    val forbruksdager: List<Forbruksdag>,
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

    fun beregn(): Beregningresultat = beregnUtbetaling()

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
            return Beregningresultat(
                utbetaling = 0,
                forbruktEgenandel = 0,
                forbruksdager = emptyList(),
            )
        } else {
            // fordel i bøtter per sats
            val dagerGruppertPåSats: Map<Beløp, List<Arbeidsdag>> = arbeidsdager.groupBy { it.sats }
            val dagerGruppertPåSatsGradert = dagerGruppertPåSats.map { ((it.key * it.value.size) * prosentfaktor) to it.value }.toList()

            // sum av alle bøtter før egenandelstrekk
            val sumFørEgenAndelstrekk = Beløp(dagerGruppertPåSatsGradert.sumOf { it.first.verdien })

            val bøtter =
                dagerGruppertPåSatsGradert.map {
                    val totalBeløp = it.first
                    val bøtteStørrelseIProsent =
                        if (sumFørEgenAndelstrekk == Beløp(0.0)) 0.0.toBigDecimal() else (totalBeløp / sumFørEgenAndelstrekk).verdien
                    val egenandel = minOf(sumFørEgenAndelstrekk, Beløp(gjenståendeEgenandel.verdien * bøtteStørrelseIProsent).avrundetBeløp)
                    Bøtte(
                        sats = it.second.first().sats,
                        arbeidsdager = it.second,
                        totalBeløp = totalBeløp,
                        egenandelProsent = bøtteStørrelseIProsent,
                        egenandel = egenandel,
                        utbetalt = (totalBeløp - egenandel).avrundetBeløp,
                    )
                }

            val forbruksdager =
                bøtter
                    .map { bøtte ->
                        bøtte.arbeidsdager.map { dag ->
                            Beregningresultat.Forbruksdag(
                                dag = dag,
                                tilUtbetaling =
                                    if (dag.dato.isEqual(
                                            bøtte.arbeidsdager.last().dato,
                                        )
                                    ) {
                                        bøtte.beløpSisteDag
                                    } else {
                                        bøtte.dagsbeløp
                                    },
                            )
                        }
                    }.flatten()
                    .sortedBy { it.dag.dato }

            val totaltUtbetalt = bøtter.sumOf { it.utbetalt.verdien }.intValueExact()
            val totaltForbruktEgenandel = bøtter.sumOf { it.egenandel.heleKroner.toInt() }
            return Beregningresultat(
                utbetaling = totaltUtbetalt,
                forbruktEgenandel = totaltForbruktEgenandel,
                forbruksdager = forbruksdager,
            )
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
