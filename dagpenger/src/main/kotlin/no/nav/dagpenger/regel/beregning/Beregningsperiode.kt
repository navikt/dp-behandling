package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.beregning.Beregning.arbeidsdag
import no.nav.dagpenger.regel.beregning.Beregning.arbeidstimer
import no.nav.dagpenger.regel.beregning.Beregning.terskel
import no.nav.dagpenger.regel.beregning.Beregningsperiode.Terskelstrategi
import no.nav.dagpenger.regel.fastsetting.DagpengensStørrelse.sats
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.antallStønadsuker
import java.math.BigDecimal
import java.time.LocalDate

internal class Beregningsperiode private constructor(
    dager: List<Dag>,
    terskelstrategi: Terskelstrategi = snitterskel,
) {
    constructor(vararg dag: Dag) : this(dag.toList())

    init {
        require(dager.size <= 14) { "En beregningsperiode kan maksimalt inneholde 14 dager" }
    }

    val sumFva = dager.mapNotNull { it.fva }.sum()
    val timerArbeidet = dager.mapNotNull { it.timerArbeidet }.sum()
    val taptArbeidstid = sumFva - timerArbeidet
    val prosentfaktor = taptArbeidstid / sumFva

    private val arbeidsdager = dager.filterIsInstance<Arbeidsdag>()
    val terskel = terskelstrategi.beregnTerskel(arbeidsdager)

    val oppfyllerKravTilTaptArbeidstid = (timerArbeidet / sumFva) <= terskel
    val utbetaling =
        arbeidsdager
            .groupBy { it.sats }
            .map { (sats, dagerMedDenneSatsen) ->
                val sumForPeriode = (sats * dagerMedDenneSatsen.size) * prosentfaktor
                val sumForDag = sumForPeriode / dagerMedDenneSatsen.size
                dagerMedDenneSatsen.forEach { it.sumTilUtbetaling = sumForDag }
                sumForPeriode
            }.sum()

    val forbruksdager =
        when (oppfyllerKravTilTaptArbeidstid) {
            true -> arbeidsdager
            false -> emptyList()
        }

    private fun interface Terskelstrategi {
        fun beregnTerskel(dager: List<Arbeidsdag>): Double
    }

    companion object {
        fun fraOpplysninger(
            meldeperiodeFraOgMed: LocalDate,
            opplysninger: Opplysninger,
        ): Beregningsperiode {
            // TODO: Finn en ekte virkningsdato
            val virkningsdato = opplysninger.finnOpplysning(antallStønadsuker).gyldighetsperiode.fom
            val fraOgMed = maxOf(meldeperiodeFraOgMed, virkningsdato)
            val dager = meldeperiodeFraOgMed.until(fraOgMed).days
            // TODO: Lag en robust logikk for å finne den lengste perioden som kan beregnes
            // val sisteStart = maxOf(virkningsdato, meldeperiodeFraOgMed)
            // val førsteSlutt = minOf(stansdato, meldeperiodeTilOgMed)
            // val periode = sisteStart..førsteSlutt
            val periode = dager..13

            return Beregningsperiode(
                periode
                    .map { meldeperiodeFraOgMed.plusDays(it.toLong()) }
                    .map { dato ->
                        opplysninger.forDato = dato
                        when (dato.dayOfWeek.value) {
                            in 1..5 -> {
                                val erArbeidsdag = opplysninger.har(arbeidsdag) && opplysninger.finnOpplysning(arbeidsdag).verdi
                                when (erArbeidsdag) {
                                    true ->
                                        Arbeidsdag(
                                            dato,
                                            opplysninger
                                                .finnOpplysning(sats)
                                                .verdi.verdien
                                                .toInt(),
                                            opplysninger.finnOpplysning(fastsattVanligArbeidstid).verdi / 5,
                                            opplysninger.finnOpplysning(arbeidstimer).verdi,
                                            opplysninger.finnOpplysning(terskel).verdi.toBigDecimal(),
                                        )

                                    false -> Fraværsdag(dato)
                                }
                            }

                            in 6..7 ->
                                Helgedag(
                                    dato,
                                    opplysninger.finnOpplysning(arbeidstimer).verdi,
                                )

                            else -> Fraværsdag(dato)
                        }
                    },
            )
        }

        private val snitterskel: Terskelstrategi =
            Terskelstrategi {
                it.sumOf { arbeidsdag -> arbeidsdag.terskel }.toDouble() / it.size
            }
    }
}

internal interface Dag {
    val dato: LocalDate
    val sats: Int?
    val fva: Double?
    val timerArbeidet: Int?
}

internal class Arbeidsdag(
    override val dato: LocalDate,
    override val sats: Int,
    override val fva: Double,
    override val timerArbeidet: Int,
    val terskel: BigDecimal,
) : Dag {
    var sumTilUtbetaling: Double = 0.0

    constructor(dato: LocalDate, sats: Int, fva: Double, timerArbeidet: Int, terskel: Double) :
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
    override val timerArbeidet: Int?,
) : Dag {
    override val sats = null
    override val fva = null
}