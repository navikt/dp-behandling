package no.nav.dagpenger.regel.fastsetting

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.verdier.Beløp
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class BarnetilleggSatsTest {
    @ParameterizedTest(name = "{0} skal gi barnetillegg på {1}")
    @CsvSource(
        "-999999999-01-01, 17",
        "2023-01-31, 17",
        "2023-02-01, 35",
        "2023-02-02, 35",
        "2023-12-31, 35",
        "2024-01-01, 36",
        "2024-01-02, 36",
        "2024-12-31, 36",
        "2025-01-01, 37",
        "2025-01-02, 37",
        "2025-12-31, 37",
        "2026-01-01, 38",
        "2026-01-02, 38",
        "2027-01-01, 38",
        "+999999999-12-31, 38",
    )
    fun `skal returnere korrekt barnetillegg for alle grenseverdier`(
        dato: LocalDate,
        forventetSats: Int,
    ) {
        BarnetilleggSats.forDato(dato) shouldBe Beløp(forventetSats)
    }
}
