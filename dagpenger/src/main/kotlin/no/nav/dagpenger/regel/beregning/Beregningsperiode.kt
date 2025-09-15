package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.summer
import java.util.SortedSet

private data class Vedtaksopplysninger(
    val gjenståendeEgenandel: Beløp,
    val meldekortdager: Set<Dag>,
    val terskelstrategi: Beregningsperiode.Terskelstrategi,
    val stønadsdagerIgjen: Int,
)

private data class Stønadsdager(
    val dager: SortedSet<Arbeidsdag>,
    val sumFva: Timer,
    val timerArbeidet: Timer,
)

private data class TaptArbeidstid(
    val prosentfaktor: Double,
    val terskel: Double,
    val oppfylt: Boolean,
)

private data class SatsGruppe(
    val sats: Beløp,
    val dager: List<Arbeidsdag>,
    val totalBeløpFørEgenandel: Beløp,
)

private data class Bøtte(
    val stønadsdager: List<Arbeidsdag>,
    val egenandel: Beløp,
    val utbetalt: Beløp,
) {
    private val antallDager = stønadsdager.size

    // Rester/fordeling for å sikre samme siste-dag-logikk:
    private val desimaldel: Beløp = Beløp(utbetalt.verdien % antallDager.toBigDecimal())
    private val dagsbeløpHeleKroner: Int =
        ((utbetalt - desimaldel) / antallDager).heleKroner.toInt()
    private val beløpSisteDagHeleKroner: Int =
        dagsbeløpHeleKroner + desimaldel.heleKroner.toInt()

    fun tilForbruksdager(): List<Beregningresultat.Forbruksdag> =
        stønadsdager.mapIndexed { index, dag ->
            val siste = index == stønadsdager.lastIndex
            val tilUtbetaling = if (siste) beløpSisteDagHeleKroner else dagsbeløpHeleKroner
            Beregningresultat.Forbruksdag(dag, tilUtbetaling)
        }
}

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
    gjenståendeEgenandel: Beløp,
    dager: Set<Dag>,
    terskelstrategi: Terskelstrategi,
    stønadsdagerIgjen: Int,
) {
    constructor(gjenståendeEgenandel: Beløp, dag: Set<Dag>, stønadsdagerIgjen: Int) : this(
        gjenståendeEgenandel = gjenståendeEgenandel,
        dager = dag,
        terskelstrategi = snitterskel,
        stønadsdagerIgjen = stønadsdagerIgjen,
    )

    init {
        require(dager.size <= 14) { "En beregningsperiode kan maksimalt inneholde 14 dager" }
    }

    // 1) Pakk inndata
    private val vedtaksopplysninger =
        Vedtaksopplysninger(
            gjenståendeEgenandel = gjenståendeEgenandel,
            meldekortdager = dager,
            terskelstrategi = terskelstrategi,
            stønadsdagerIgjen = stønadsdagerIgjen,
        )

    // 2) Filtrer/ordne dager og summer timer
    private val stønadsdager =
        run {
            val stønadsdager =
                vedtaksopplysninger.meldekortdager
                    .filterIsInstance<Arbeidsdag>()
                    .take(minOf(vedtaksopplysninger.meldekortdager.size, vedtaksopplysninger.stønadsdagerIgjen))
                    .toSortedSet()

            val sumFva = vedtaksopplysninger.meldekortdager.mapNotNull { it.fva }.summer()
            val timerArbeidet = vedtaksopplysninger.meldekortdager.mapNotNull { it.timerArbeidet }.summer()

            Stønadsdager(
                dager = stønadsdager,
                sumFva = sumFva,
                timerArbeidet = timerArbeidet,
            )
        }

    // 3) Beregn tapt arbeidstid/prosentfaktor og terskel
    private val taptArbeidstid =
        run {
            val prosentfaktor = ((stønadsdager.sumFva - stønadsdager.timerArbeidet) / stønadsdager.sumFva).timer
            val terskel: Double = (100 - vedtaksopplysninger.terskelstrategi.beregnTerskel(stønadsdager.dager)) / 100
            val oppfylt = (stønadsdager.timerArbeidet / stønadsdager.sumFva).timer <= terskel
            TaptArbeidstid(prosentfaktor, terskel, oppfylt)
        }

    val oppfyllerKravTilTaptArbeidstid: Boolean = taptArbeidstid.oppfylt

    // 4) Endelig resultat (immutable, beregnes én gang)
    val resultat: Beregningresultat = beregn()

    private fun beregn(): Beregningresultat {
        if (!oppfyllerKravTilTaptArbeidstid) {
            return Beregningresultat(utbetaling = 0, forbruktEgenandel = 0, forbruksdager = emptyList())
        }

        // 4.1) Grupper stønadsdagene per sats og beregn totalbeløp pr. gruppe før egenandel
        val grupper: List<SatsGruppe> =
            stønadsdager.dager
                .groupBy { it.sats }
                .map { (sats, dager) ->
                    val total = ((sats * dager.size) * taptArbeidstid.prosentfaktor)
                    SatsGruppe(
                        sats = sats,
                        dager = dager,
                        totalBeløpFørEgenandel = total,
                    )
                }

        // 4.2) Summer totalen før egenandel
        val sumFørEgenandel = Beløp(grupper.sumOf { it.totalBeløpFørEgenandel.verdien })

        // 4.3) Fordel egenandel proporsjonalt i "bøtter" og beregn utbetaling pr bøtte
        val bøtter: List<Bøtte> =
            grupper.map { gruppe ->
                val andelProsent =
                    if (sumFørEgenandel == Beløp(0.0)) {
                        0.0.toBigDecimal()
                    } else {
                        (gruppe.totalBeløpFørEgenandel / sumFørEgenandel).verdien
                    }

                val egenandelForBøtte =
                    minOf(
                        sumFørEgenandel,
                        Beløp(vedtaksopplysninger.gjenståendeEgenandel.verdien * andelProsent).avrundetBeløp,
                    )

                val utbetalt = (gruppe.totalBeløpFørEgenandel - egenandelForBøtte).avrundetBeløp

                Bøtte(
                    stønadsdager = gruppe.dager,
                    egenandel = egenandelForBøtte,
                    utbetalt = utbetalt,
                )
            }

        // 4.4) Forbruksdager fra alle bøtter, sortert på dag
        val forbruksdager =
            bøtter
                .flatMap { it.tilForbruksdager() }
                .sortedBy { it.dag }

        // 4.5) Summer sluttbeløp
        val totalUtbetaling = bøtter.sumOf { it.utbetalt.verdien }.intValueExact()
        val totalForbruktEgenandel = bøtter.sumOf { it.egenandel.heleKroner.toInt() }

        return Beregningresultat(
            utbetaling = totalUtbetaling,
            forbruktEgenandel = totalForbruktEgenandel,
            forbruksdager = forbruksdager,
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
