package no.nav.dagpenger.regel.beregning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer.Companion.timer
import no.nav.dagpenger.regel.regelsett.beregning.Arbeidsdag
import no.nav.dagpenger.regel.regelsett.beregning.Beregningsperiode
import no.nav.dagpenger.regel.regelsett.beregning.Dag
import org.junit.jupiter.api.Test

class BortfallBeregningsperiodeTest {
    @Test
    fun `uten bortfall beregnes alt normalt`() {
        val dager = lagArbeidsdager(5)
        val beregning = Beregningsperiode(Beløp(0), dager, stønadsdagerIgjen = 52, bortfallsdagerIgjen = 0)

        beregning.resultat.beregningsdager.size shouldBe 5
        beregning.resultat.beregningsdager.none { it.erBortfall } shouldBe true
        beregning.resultat.utbetaling shouldBe Beløp(500) // 5 dager * 100 kr sats * 1.0 prosentfaktor
    }

    @Test
    fun `fullt bortfall gir 0 utbetaling men alle dager er forbruk`() {
        val dager = lagArbeidsdager(5)
        val beregning = Beregningsperiode(Beløp(0), dager, stønadsdagerIgjen = 52, bortfallsdagerIgjen = 10)

        beregning.resultat.beregningsdager.size shouldBe 5
        beregning.resultat.beregningsdager.all { it.erBortfall } shouldBe true
        beregning.resultat.utbetaling shouldBe Beløp(0)
        beregning.resultat.forbruktEgenandel shouldBe Beløp(0)
    }

    @Test
    fun `delvis bortfall - tidligste dager er bortfall, resten utbetales`() {
        val dager = lagArbeidsdager(5)
        val beregning = Beregningsperiode(Beløp(0), dager, stønadsdagerIgjen = 52, bortfallsdagerIgjen = 2)

        val forbruksdager = beregning.resultat.beregningsdager.sortedBy { it.dag.dato }
        forbruksdager.size shouldBe 5

        // De 2 første dagene er bortfall
        forbruksdager[0].erBortfall shouldBe true
        forbruksdager[0].tilUtbetaling shouldBe Beløp(0)
        forbruksdager[1].erBortfall shouldBe true
        forbruksdager[1].tilUtbetaling shouldBe Beløp(0)

        // De 3 siste er normal utbetaling
        forbruksdager[2].erBortfall shouldBe false
        forbruksdager[3].erBortfall shouldBe false
        forbruksdager[4].erBortfall shouldBe false

        // Total utbetaling er kun for 3 dager
        beregning.resultat.utbetaling shouldBe Beløp(300)
    }

    @Test
    fun `bortfall forbruker ikke egenandel`() {
        val dager = lagArbeidsdager(5)
        val gjenståendeEgenandel = Beløp(150)

        // Uten bortfall - egenandel forbrukes
        val utenBortfall = Beregningsperiode(gjenståendeEgenandel, dager, stønadsdagerIgjen = 52, bortfallsdagerIgjen = 0)
        utenBortfall.resultat.forbruktEgenandel shouldBe Beløp(150)

        // Med fullt bortfall - egenandel forbrukes IKKE
        val medBortfall = Beregningsperiode(gjenståendeEgenandel, dager, stønadsdagerIgjen = 52, bortfallsdagerIgjen = 10)
        medBortfall.resultat.forbruktEgenandel shouldBe Beløp(0)
        medBortfall.resultat.gjenståendeEgenandel shouldBe gjenståendeEgenandel
    }

    @Test
    fun `delvis bortfall - egenandel forbrukes bare fra ikke-bortfallsdager`() {
        val dager = lagArbeidsdager(5)
        val gjenståendeEgenandel = Beløp(150)

        // Med 2 bortfallsdager - egenandel fordeles kun over 3 utbetalingsdager
        val beregning = Beregningsperiode(gjenståendeEgenandel, dager, stønadsdagerIgjen = 52, bortfallsdagerIgjen = 2)

        // 3 utbetalingsdager * 100 kr = 300 brutto. Egenandel 150 trekkes fra dette.
        beregning.resultat.forbruktEgenandel shouldBe Beløp(150)
        beregning.resultat.utbetaling shouldBe Beløp(150) // 300 - 150
    }

    private fun lagArbeidsdager(antall: Int): Set<Dag> =
        (6..(5 + antall))
            .map { dag ->
                Arbeidsdag(
                    dato = dag.januar(2025),
                    sats = Beløp(100),
                    fva = 7.5.timer,
                    timerArbeidet = 0.0.timer,
                    terskel = 50.0,
                )
            }.toSortedSet()
}
