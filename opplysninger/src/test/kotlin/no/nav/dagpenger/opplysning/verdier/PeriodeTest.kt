package no.nav.dagpenger.opplysning.verdier

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.dagpenger.opplysning.januar
import org.junit.jupiter.api.Test

class PeriodeTest {
    @Test
    fun `kan iterere over en periode`() {
        val periode = Periode(1.januar, 3.januar)

        periode shouldHaveSize 3

        periode shouldContainExactly listOf(1.januar, 2.januar, 3.januar)
    }

    @Test
    fun `en periode med samme fom og tom skal bare være en dag`() {
        val periode = Periode(1.januar, 1.januar)

        periode shouldHaveSize 1

        periode shouldContainExactly listOf(1.januar)
    }
}
