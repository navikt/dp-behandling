package no.nav.dagpenger.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.regel.beregning.Arbeidsdag
import no.nav.dagpenger.regel.beregning.Beregningsperiode
import no.nav.dagpenger.regel.beregning.Fraværsdag
import no.nav.dagpenger.regel.beregning.Helgedag
import org.junit.jupiter.api.Test
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
                300.0,
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
                300.0,
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
                300.0,
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
        /*
                val forbrukt = Opplysningstype.somBoolsk("Forbrukt dag")
                val opplysninger = Opplysninger()

                val periode = opplysninger.finnOpplysning("Periode")
                val forbruktedager = opplysninger.finnAlle().filter { it.type == forbrukt }
                val gjenstående = periode - forbruktedager.size

                val periode = fraOgMed.datesUntil(fraOgMed.plusDays(14)).toList()
                for (dato in periode) {
                    opplysninger.forDato = dato
                    val sats = opplysninger.finnOpplysning("Sats")
                    val fva = opplysninger.finnOpplysning("fva")
                    val terskel = opplysninger.finnOpplysning("terskel")

                    Arbeidsdag(dato, sats, fva, 0, terskel)
                }

                beregningsperiode.dagerMedForbruk.forEach { dag ->
                    opplysninger.leggTil(Faktum(forbrukt, true, Gyldighetsperiode(fom = dag.dato, tom = dag.dato)))
                    opplysninger.leggTil(Faktum(utbetaling, utbetalt, Gyldighetsperiode(fom = dag.dato, tom = dag.dato)))
                }
                opplysninger.leggTil(Faktum(utbetalt, beregningsperiode.utbetaling, Gyldighetsperiode(fom = fraOgMed, tom = tilOgMed)))*/

        beregningsperiode.sumFva shouldBe (37.5 * 2) - 7.5 // 67,5 med trekk fra fraværsdag
        beregningsperiode.timerArbeidet shouldBe 19
        beregningsperiode.taptArbeidstid shouldBe 48.5

        beregningsperiode.prosentfaktor shouldBe 0.7185185185185186

        // Sjekke terskel for periode
        beregningsperiode.oppfyllerKravTilTaptArbeidstid shouldBe true

        beregningsperiode.utbetaling shouldBe 11280.74074074074
    }

    @Test
    fun `oppfyller kravet til tapt arbeidsttid for en periode med endring av terskel midt i perioden`() {
        val fraOgMed = LocalDate.now().minusDays(14)

        val terskel = 0.5
        val nyTerskel = 0.6
        val fva = 8.0
        val beregningsperiode =
            Beregningsperiode(
                300.0,
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
