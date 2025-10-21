package no.nav.dagpenger.opplysning.verdier

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.ints.shouldBeExactly
import no.nav.dagpenger.opplysning.januar
import org.junit.jupiter.api.Test

class PeriodeTest {
    @Test
    fun `likhet test`() {
        val periode1 = Periode(1.januar, 3.januar)
        val periode2 = Periode(1.januar, 3.januar)
        val periode3 = Periode(2.januar, 4.januar)

        periode1 shouldBeEqual periode2
        periode2 shouldBeEqual periode1
        periode3 shouldNotBeEqual periode2
        periode3 shouldNotBeEqual periode1

        periode1.hashCode() shouldBeExactly periode1.hashCode()
        periode1.hashCode() shouldBeExactly periode2.hashCode()
        periode3.hashCode() shouldNotBeEqual periode2.hashCode()
        periode3.hashCode() shouldNotBeEqual periode1.hashCode()
    }

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
