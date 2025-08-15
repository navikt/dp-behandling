package no.nav.dagpenger.regel.beregning

import io.kotest.matchers.equals.shouldBeEqual
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.opplysning.verdier.enhet.Timer
import kotlin.test.Test

class DagerTest {
    @Test
    fun `sorterbar i kronologisk rekkefølge`() {
        val dager =
            listOf(
                Arbeidsdag(2.januar(2020), Beløp(100.0), Timer(7.5), Timer(7.5), 0.0),
                Arbeidsdag(5.januar(2020), Beløp(100.0), Timer(7.5), Timer(7.5), 0.0),
                Fraværsdag(3.januar(2020)),
                Helgedag(1.januar(2020), Timer(0.0)),
            )

        val sorterteDager = dager.sorted()

        sorterteDager.first().dato shouldBeEqual 1.januar(2020)
        sorterteDager.last().dato shouldBeEqual 5.januar(2020)
    }
}
