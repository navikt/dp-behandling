package no.nav.dagpenger.behandling.modell

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.modell.Beregningsperiode.Terskelstrategi
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BeregningTest {
    @Test
    fun `vi kan lage en liste med dager for en periode`() {
        val innvilgelsesdato = LocalDate.now().minusDays(10)
        val fraOgMedPeriode = LocalDate.now().minusDays(14)
        val fraOgMed = listOf(innvilgelsesdato, fraOgMedPeriode).max()

        val terskel = 0.5
        val beregningsperiode =
            Beregningsperiode(
                // Arbeidsdag(fraOgMed, 100, 7.5, 7, terskel),
                // Arbeidsdag(fraOgMed.plusDays(1), 100, 7.5, 7, terskel),
                // Arbeidsdag(fraOgMed.plusDays(2), 100, 7.5, 7, terskel),
                // Arbeidsdag(fraOgMed.plusDays(3), 100, 7.5, 7, terskel),
                Arbeidsdag(fraOgMed.plusDays(1), 100, 7.5, 7, terskel),
                Helgedag(fraOgMed.plusDays(2), 0),
                Helgedag(fraOgMed.plusDays(3), 0),
                Arbeidsdag(fraOgMed.plusDays(4), 100, 7.5, 7, terskel),
                Arbeidsdag(fraOgMed.plusDays(5), 100, 7.5, 7, terskel),
                Arbeidsdag(fraOgMed.plusDays(6), 100, 7.5, 7, terskel),
                Arbeidsdag(fraOgMed.plusDays(7), 100, 7.5, 7, terskel),
                Fraværsdag(fraOgMed.plusDays(8)),
                Helgedag(fraOgMed.plusDays(9), 0),
                Helgedag(fraOgMed.plusDays(10), 2),
            )

        beregningsperiode.sumFva shouldBe 37.5
        beregningsperiode.timerArbeidet shouldBe 37
        beregningsperiode.taptArbeidstid shouldBe 0.5

        beregningsperiode.prosentfaktor shouldBe 0.013333333333333334

        // Sjekke terskel for periode
        beregningsperiode.oppfyllerKravTilTaptArbeidstid shouldBe false
    }

    @Test
    fun `oppfyller kravet til tapt arbeidsttid for en periode`() {
        val fraOgMed = LocalDate.now().minusDays(14)

        val terskel = 0.5
        val beregningsperiode =
            Beregningsperiode(
                Arbeidsdag(fraOgMed, 100, 7.5, 3, terskel),
                Arbeidsdag(fraOgMed.plusDays(1), 100, 7.5, 3, terskel),
                Arbeidsdag(fraOgMed.plusDays(2), 100, 7.5, 3, terskel),
                Arbeidsdag(fraOgMed.plusDays(3), 100, 7.5, 0, terskel),
                Arbeidsdag(fraOgMed.plusDays(4), 100, 7.5, 0, terskel),
                Helgedag(fraOgMed.plusDays(5), 0),
                Helgedag(fraOgMed.plusDays(6), 0),
                Arbeidsdag(fraOgMed.plusDays(7), 100, 7.5, 4, terskel),
                Arbeidsdag(fraOgMed.plusDays(8), 100, 7.5, 0, terskel),
                Arbeidsdag(fraOgMed.plusDays(9), 100, 7.5, 2, terskel),
                Arbeidsdag(fraOgMed.plusDays(10), 100, 7.5, 0, terskel),
                Fraværsdag(fraOgMed.plusDays(11)),
                Helgedag(fraOgMed.plusDays(12), 2),
                Helgedag(fraOgMed.plusDays(13), 2),
            )

        beregningsperiode.sumFva shouldBe (37.5 * 2) - 7.5 // Trekk fra fraværsdag
        beregningsperiode.timerArbeidet shouldBe 19
        beregningsperiode.taptArbeidstid shouldBe 48.5

        beregningsperiode.prosentfaktor shouldBe 0.7185185185185186

        // Sjekke terskel for periode
        beregningsperiode.oppfyllerKravTilTaptArbeidstid shouldBe true
    }

    @Test
    fun `gradering`() {
        val fraOgMed = LocalDate.now().minusDays(14)

        val terskel = 0.5
        val beregningsperiode =
            Beregningsperiode(
                Arbeidsdag(fraOgMed, 500, 7.5, 3, terskel),
                Arbeidsdag(fraOgMed.plusDays(1), 500, 7.5, 3, terskel),
                Arbeidsdag(fraOgMed.plusDays(2), 500, 7.5, 3, terskel),
                Arbeidsdag(fraOgMed.plusDays(3), 500, 7.5, 0, terskel),
                Arbeidsdag(fraOgMed.plusDays(4), 500, 7.5, 0, terskel),
                Helgedag(fraOgMed.plusDays(5), 0),
                Helgedag(fraOgMed.plusDays(6), 0),
                Arbeidsdag(fraOgMed.plusDays(7), 200, 7.5, 4, terskel),
                Arbeidsdag(fraOgMed.plusDays(8), 200, 7.5, 0, terskel),
                Arbeidsdag(fraOgMed.plusDays(9), 200, 7.5, 2, terskel),
                Arbeidsdag(fraOgMed.plusDays(10), 200, 7.5, 0, terskel),
                Fraværsdag(fraOgMed.plusDays(11)),
                Helgedag(fraOgMed.plusDays(12), 2),
                Helgedag(fraOgMed.plusDays(13), 2),
            )

        beregningsperiode.sumFva shouldBe (37.5 * 2) - 7.5 // 67,5 med trekk fra fraværsdag
        beregningsperiode.timerArbeidet shouldBe 19
        beregningsperiode.taptArbeidstid shouldBe 48.5

        beregningsperiode.prosentfaktor shouldBe 0.7185185185185186

        // Sjekke terskel for periode
        beregningsperiode.oppfyllerKravTilTaptArbeidstid shouldBe true

        beregningsperiode.utbetaling shouldBe 11280.74074074074
        beregningsperiode.sumPerDag shouldBe 1253.4156378600824
    }

    @Test
    fun `oppfyller kravet til tapt arbeidsttid for en periode med endring av terskel midt i perioden`() {
        val fraOgMed = LocalDate.now().minusDays(14)

        val terskel = 0.5
        val nyTerskel = 0.6
        val fva = 8.0
        val beregningsperiode =
            Beregningsperiode(
                Arbeidsdag(fraOgMed, 100, fva, 7, terskel),
                Arbeidsdag(fraOgMed.plusDays(1), 100, fva, 7, terskel),
                Arbeidsdag(fraOgMed.plusDays(2), 100, fva, 7, terskel),
                Arbeidsdag(fraOgMed.plusDays(3), 100, fva, 0, terskel),
                Arbeidsdag(fraOgMed.plusDays(4), 100, fva, 0, terskel),
                Helgedag(fraOgMed.plusDays(5), 0),
                Helgedag(fraOgMed.plusDays(6), 0),
                Arbeidsdag(fraOgMed.plusDays(7), 100, fva, 7, nyTerskel),
                Arbeidsdag(fraOgMed.plusDays(8), 100, fva, 7, nyTerskel),
                Arbeidsdag(fraOgMed.plusDays(9), 100, fva, 7, nyTerskel),
                Arbeidsdag(fraOgMed.plusDays(10), 100, fva, 0, nyTerskel),
                Arbeidsdag(fraOgMed.plusDays(11), 100, fva, 0, nyTerskel),
                Helgedag(fraOgMed.plusDays(12), 0),
                Helgedag(fraOgMed.plusDays(13), 0),
            )

        beregningsperiode.timerArbeidet shouldBe 42
        beregningsperiode.taptArbeidstid shouldBe 38.0
        beregningsperiode.terskel shouldBe 0.55

        // Sjekke terskel for periode
        beregningsperiode.oppfyllerKravTilTaptArbeidstid shouldBe true
    }
}

class Beregningsperiode private constructor(
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

    private companion object {
        private val snitterskel: Terskelstrategi =
            Terskelstrategi {
                it.sumOf { arbeidsdag -> arbeidsdag.terskel }.toDouble() / it.size
            }
    }
}

interface Dag {
    val dato: LocalDate
    val sats: Int?
    val fva: Double?
    val timerArbeidet: Int?
}

class Arbeidsdag(
    override val dato: LocalDate,
    override val sats: Int,
    override val fva: Double,
    override val timerArbeidet: Int,
    val terskel: BigDecimal,
) : Dag {
    constructor(dato: LocalDate, sats: Int, fva: Double, timerArbeidet: Int, terskel: Double) :
        this(dato, sats, fva, timerArbeidet, BigDecimal.valueOf(terskel))
}

class Fraværsdag(
    override val dato: LocalDate,
) : Dag {
    override val sats = null
    override val fva = null
    override val timerArbeidet = null
}

class Helgedag(
    override val dato: LocalDate,
    override val timerArbeidet: Int?,
) : Dag {
    override val sats = null
    override val fva = null
}
