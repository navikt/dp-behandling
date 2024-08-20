package no.nav.dagpenger.behandling.modell

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningTest {
    @Test
    fun `vi kan lage en liste med dager for en periode`() {
        val fraOgMed = LocalDate.now().minusDays(14)
        val tilOgMed = LocalDate.now()
        val periode: ClosedRange<LocalDate> = fraOgMed..tilOgMed

        val terskel = 0.5
        val beregningsperiode =
            Beregningsperiode(
                Rettighetsdag(fraOgMed, 100, 7.5, 7, terskel),
                Rettighetsdag(fraOgMed.plusDays(1), 100, 7.5, 7, terskel),
                Rettighetsdag(fraOgMed.plusDays(2), 100, 7.5, 7, terskel),
                Rettighetsdag(fraOgMed.plusDays(3), 100, 7.5, 7, terskel),
                Rettighetsdag(fraOgMed.plusDays(4), 100, 7.5, 7, terskel),
                Helgedag(fraOgMed.plusDays(5), 0),
                Helgedag(fraOgMed.plusDays(6), 0),
                Rettighetsdag(fraOgMed.plusDays(7), 100, 7.5, 7, terskel),
                Rettighetsdag(fraOgMed.plusDays(8), 100, 7.5, 7, terskel),
                Rettighetsdag(fraOgMed.plusDays(9), 100, 7.5, 7, terskel),
                Rettighetsdag(fraOgMed.plusDays(10), 100, 7.5, 7, terskel),
                Fraværsdag(fraOgMed.plusDays(11), terskel),
                Helgedag(fraOgMed.plusDays(12), 0),
                Helgedag(fraOgMed.plusDays(13), 2),
            )

        beregningsperiode.fva shouldBe (37.5 * 2) - 7.5
        beregningsperiode.timerArbeidet shouldBe 65
        beregningsperiode.taptArbeidstid shouldBe 2.5

        beregningsperiode.prosentfaktor shouldBe 0.037037037037037035

        // Sjekke terskel for periode
        beregningsperiode.oppfyllerKravTilTaptArbeidstid shouldBe false
    }
}

class Beregningsperiode(
    private val dager: List<Dag>,
) {
    constructor(vararg dag: Dag) : this(dag.toList())

    val fva: Double get() = dager.mapNotNull { it.fva }.sum()
    val timerArbeidet get() = dager.mapNotNull { it.timerArbeidet }.sum()
    val taptArbeidstid get() = fva - timerArbeidet
    val prosentfaktor get() = taptArbeidstid / fva

    private val antallRettighetsdager get() = dager.filterIsInstance<Rettighetsdag>().size
    private val fvaPerDag get() = fva / antallRettighetsdager
    val oppfyllerKravTilTaptArbeidstid get() = (timerArbeidet / (antallRettighetsdager * fvaPerDag)) <= prosentfaktor
}

interface Dag {
    val dato: LocalDate
    val sats: Int?
    val fva: Double?
    val timerArbeidet: Int?
}

abstract class Hverdag(
    val terskel: Double,
) : Dag {
    val dagsbeløpTilUtebetaling: Int? = null
}

class Rettighetsdag(
    override val dato: LocalDate,
    override val sats: Int,
    override val fva: Double,
    override val timerArbeidet: Int,
    terskel: Double,
) : Hverdag(terskel)

class Fraværsdag(
    override val dato: LocalDate,
    terskel: Double,
) : Hverdag(terskel) {
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
