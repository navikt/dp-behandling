package no.nav.dagpenger.regel.beregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.verdier.Beløp
import org.junit.jupiter.api.Test

class BeregningsperiodeTest {
    @Test
    fun `beregning kan ikke gå over lengre periode enn 14 dager`() {
        val fraværsdager = (1..18).map { dato -> Fraværsdag(dato.januar(2025)) }.toSet()
        shouldThrow<IllegalArgumentException> {
            Beregningsperiode(Beløp(0.0), fraværsdager, 52)
        }
    }

    @Test
    fun `håndterer perioder uten arbeidsdager`() {
        val fraværsdager = (1..14).map { dato -> Fraværsdag(dato.januar(2025)) }.toSet()
        val beregning = Beregningsperiode(Beløp(0.0), fraværsdager, 52)

        beregning.resultat.forbruksdager.shouldBeEmpty()
        beregning.resultat.utbetaling shouldBe Beløp(0.0)
        beregning.resultat.oppfyllerKravTilTaptArbeidstid shouldBe true
    }
}
