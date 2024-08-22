package no.nav.dagpenger.regel.beregning

import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.beregning.Beregning.arbeidsdag
import no.nav.dagpenger.regel.beregning.Beregning.terskel
import no.nav.dagpenger.regel.beregning.Beregningsperiode.Terskelstrategi
import no.nav.dagpenger.regel.fastsetting.DagpengensStørrelse.sats
import java.math.BigDecimal
import java.time.LocalDate

internal class Beregningsperiode private constructor(
    private val dager: List<Dag>,
    private val terskelstrategi: Terskelstrategi = snitterskel,
) {
    constructor(vararg dag: Dag) : this(dag.toList())

    init {
        require(dager.size <= 14) { "En beregningsperiode kan maksimalt inneholde 14 dager" }
    }

    val sumFva: Double get() = dager.mapNotNull { it.fva }.sum()
    val timerArbeidet get() = dager.mapNotNull { it.timerArbeidet }.sum()

    val taptArbeidstid get() = sumFva - timerArbeidet
    val prosentfaktor get() = taptArbeidstid / sumFva
    private val rettighetsdager get() = dager.filterIsInstance<Arbeidsdag>()
    val terskel get() = terskelstrategi.beregnTerskel(rettighetsdager)
    val oppfyllerKravTilTaptArbeidstid get() = (timerArbeidet / sumFva) <= terskel

    private val arbeidsdager get() = dager.filterIsInstance<Arbeidsdag>()
    val utbetaling
        get() =
            arbeidsdager
                .groupBy { it.sats }
                .map { (sats, dagerMedDenneSatsen) ->
                    val sumSats = (sats * dagerMedDenneSatsen.size)
                    sumSats * dagerMedDenneSatsen.size * prosentfaktor
                }.sum()
    val sumPerDag get() = utbetaling / arbeidsdager.size

    private fun interface Terskelstrategi {
        fun beregnTerskel(dager: List<Arbeidsdag>): Double
    }

    companion object {
        fun fraOpplysninger(
            meldeperiodeFraOgMed: LocalDate,
            opplysninger: Opplysninger,
        ): Beregningsperiode =
            Beregningsperiode(
                (0..13)
                    .map { meldeperiodeFraOgMed.plusDays(it.toLong()) }
                    .map { dato ->
                        opplysninger.forDato = dato
                        when (dato.dayOfWeek.value) {
                            in 1..5 -> {
                                Arbeidsdag(
                                    dato,
                                    opplysninger
                                        .finnOpplysning(sats)
                                        .verdi.verdien
                                        .toInt(),
                                    opplysninger.finnOpplysning(fastsattVanligArbeidstid).verdi,
                                    opplysninger.finnOpplysning(arbeidsdag).verdi,
                                    opplysninger.finnOpplysning(terskel).verdi.toBigDecimal(),
                                )
                            }

                            in 6..7 ->
                                Helgedag(
                                    dato,
                                    opplysninger.finnOpplysning(arbeidsdag).verdi,
                                )

                            else -> Fraværsdag(dato)
                        }
                    },
            )

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
