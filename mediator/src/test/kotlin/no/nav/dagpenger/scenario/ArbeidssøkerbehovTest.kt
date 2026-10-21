package no.nav.dagpenger.scenario

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.verdier.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ArbeidssøkerbehovTest {
    @Test
    fun `svarer med registrerte perioder og hull fra innhentingsdato`() {
        val fraOgMed = LocalDate.parse("2020-01-01")
        val registrertePerioder =
            listOf(
                Periode(LocalDate.parse("2019-12-30"), LocalDate.parse("2020-01-03")),
                Periode(LocalDate.parse("2020-01-08"), LocalDate.parse("2020-01-10")),
                Periode(LocalDate.parse("2020-03-01"), LocalDate.MAX),
            )

        registrertePerioder.somArbeidssøkerbehov(fraOgMed) shouldBe
            listOf(
                mapOf(
                    "verdi" to true,
                    "gyldigFraOgMed" to LocalDate.parse("2019-12-30"),
                    "gyldigTilOgMed" to LocalDate.parse("2020-01-03"),
                ),
                mapOf(
                    "verdi" to false,
                    "gyldigFraOgMed" to LocalDate.parse("2020-01-04"),
                    "gyldigTilOgMed" to LocalDate.parse("2020-01-07"),
                ),
                mapOf(
                    "verdi" to true,
                    "gyldigFraOgMed" to LocalDate.parse("2020-01-08"),
                    "gyldigTilOgMed" to LocalDate.parse("2020-01-10"),
                ),
                mapOf(
                    "verdi" to false,
                    "gyldigFraOgMed" to LocalDate.parse("2020-01-11"),
                    "gyldigTilOgMed" to LocalDate.parse("2020-02-29"),
                ),
                mapOf(
                    "verdi" to true,
                    "gyldigFraOgMed" to LocalDate.parse("2020-03-01"),
                    "gyldigTilOgMed" to LocalDate.MAX,
                ),
            )
    }

    @Test
    fun `svarer med usann fra innhentingsdato når ingen registreringsperioder finnes`() {
        val fraOgMed = LocalDate.parse("2020-01-01")

        emptyList<Periode>().somArbeidssøkerbehov(fraOgMed) shouldBe
            listOf(
                mapOf(
                    "verdi" to false,
                    "gyldigFraOgMed" to fraOgMed,
                    "gyldigTilOgMed" to LocalDate.MAX,
                ),
            )
    }
}
