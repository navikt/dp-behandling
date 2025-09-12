package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer
import java.math.BigDecimal
import java.time.LocalDate

class Bøtteholder(
    val arbeidsdag: Arbeidsdag,
) {
    var beløpTilFordeling: Beløp = Beløp(0.0)
        internal set

    var prosentTrekkForEgenandel: Beløp = Beløp(0.0)
        internal set

    var totalEgenandel: Beløp = Beløp(0.0)
        internal set

    val utbetaling get() = beløpTilFordeling - egenandelTrukket
    val egenandelTrukket get() = totalEgenandel * prosentTrekkForEgenandel
}

data class Blurp(
    val dag: LocalDate,
    val sats: Beløp,
    val utbetalt: Int,
    val egenandel: Beløp,
)

data class Bøtte(
    val sats: Beløp,
    val arbeidsdager: List<Arbeidsdag>,
    val totalBeløp: Beløp,
    val egenandelProsent: BigDecimal,
    val egenandel: Beløp,
    val utbetalt: Beløp,
    val avrundetBeløpPerDag: Int = (utbetalt / arbeidsdager.size).avrundetNedoverBeløp.verdien.toInt(),
    val reminder: Beløp = Beløp(utbetalt.verdien % arbeidsdager.size.toBigDecimal()),
    val dagsbeløp: Beløp = (utbetalt - reminder) / arbeidsdager.size,
    val beløpSisteDag: Beløp = dagsbeløp + reminder,
    val totaltUtbetalt: Int = avrundetBeløpPerDag * arbeidsdager.size,
    val egenandelPerDag: Beløp = egenandel / arbeidsdager.size,
    val rest: Beløp = utbetalt - Beløp(totaltUtbetalt),
)

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

    // fordel i bøtter per sats
    private val dagerGruppertPåSats: Map<Beløp, List<Arbeidsdag>> = arbeidsdager.groupBy { it.sats }
    private val dagerGruppertPåSatsGradert = dagerGruppertPåSats.map { ((it.key * it.value.size) * prosentfaktor) to it.value }.toList()

    // sum av alle bøtter før egenandelstrekk
    private val sumFørEgenAndelstrekk = Beløp(dagerGruppertPåSatsGradert.sumOf { it.first.verdien })

    private val bøtter =
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

    val forbruktEgenandel = bøtter.sumOf { it.egenandel.verdien }

//    val nyeDager =
//        bøtter
//            .map { b ->
//                b.arbeidsdager.map { d ->
//                    Blurp(
//                        dag = d.dato,
//                        sats = b.sats,
//                        utbetalt =
//                            if (d.dato.isEqual(
//                                    b.arbeidsdager.last().dato,
//                                )
//                            ) {
//                                b.beløpSisteDag.verdien.toInt()
//                            } else {
//                                b.avrundetBeløpPerDag
//                            },
//                        egenandel = b.egenandelPerDag,
//                    )
//                }
//            }.flatten()
//            .sortedBy { it.dag }

    val arbdager =
        bøtter
            .map { bøtte ->
                bøtte.arbeidsdager.map { dag ->
                    dag.dagsbeløp = if (dag.dato.isEqual(bøtte.arbeidsdager.last().dato)) bøtte.beløpSisteDag else bøtte.dagsbeløp
                    dag
                }
            }.flatten()
            .sortedBy { it.dato }

//    private val egenandelTrekkPerBøtte =
//        gradertPerSatsperiode
//            .map {
//                val bøttetrekk = it.first / sumFørEgenAndelstrekk
//                it.second.map {
//                    Bøtteholder(it).also {
//                        it.beløpTilFordeling = sumFørEgenAndelstrekk / arbeidsdager.size
//                        it.prosentTrekkForEgenandel = bøttetrekk
//                        it.totalEgenandel = gjenståendeEgenandel
//                    }
//                }
//            }.toList()
    // private val potensielSum: List<Beløp> = bøtter.map { (it.key * it.value.size) * prosentfaktor }

// val utbetalingHelePerioden = maxOf(Beløp(0.0), Beløp(potensielSum.sumOf { it.verdien }) - gjenståendeEgenandel).avrundetBeløp
    //  private val perDagFordeling: Beløp = utbetalingHelePerioden / arbeidsdager.size

    private val timerArbeidet = dager.mapNotNull { it.timerArbeidet }.summer()
    val terskel = (100 - terskelstrategi.beregnTerskel(arbeidsdager)) / 100
    val oppfyllerKravTilTaptArbeidstid = (timerArbeidet / sumFva).timer <= terskel

    val utbetaling = forbruksdager.sumOf { it.dagsbeløp.verdien }.toInt()

    // TODO: Forbruksdager må filtreres ytterligere for å ta hensyn til _faktisk_ forbruk (ekempelvis ved sanksjonsdager)
    val forbruksdager = if (oppfyllerKravTilTaptArbeidstid) beregnUtbetaling() else emptyList()

    private fun arbeidsdager(dager: Set<Dag>): Set<Arbeidsdag> {
        val arbeidsdager = dager.filterIsInstance<Arbeidsdag>()
        return arbeidsdager.subList(0, minOf(arbeidsdager.size, stønadsdagerIgjen)).toSortedSet()
    }

    private fun beregnProsentfaktor(dager: Set<Dag>): Timer {
        val timerArbeidet: Timer = dager.mapNotNull { it.timerArbeidet }.summer()
        return (sumFva - timerArbeidet) / sumFva
    }

    private fun beregnUtbetaling(): List<Arbeidsdag> {
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

        return bøtter
            .map { bøtte ->
                bøtte.arbeidsdager.map { dag ->
                    dag.dagsbeløp = if (dag.dato.isEqual(bøtte.arbeidsdager.last().dato)) bøtte.beløpSisteDag else bøtte.dagsbeløp
                    dag
                }
            }.flatten()
            .sortedBy { it.dato }
    }

    internal fun interface Terskelstrategi {
        fun beregnTerskel(dager: Set<Arbeidsdag>): Double
    }

    companion object {
        private val snitterskel: Terskelstrategi =
            Terskelstrategi { it.sumOf { arbeidsdag -> arbeidsdag.terskel }.toDouble() / it.size }
    }
}
