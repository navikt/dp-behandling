package no.nav.dagpenger.behandling.helpers.scenario

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.behandling.februar
import no.nav.dagpenger.behandling.januar
import no.nav.dagpenger.behandling.juli
import no.nav.dagpenger.behandling.juni
import no.nav.dagpenger.behandling.mai
import kotlin.test.Test

class MeldesyklusTest {
    @Test
    fun `finner meldesyklus for en vakker dag`() {
        val syklus = Meldesyklus(1.januar(2024))

        syklus.periode(1).fraOgMed shouldBe 1.januar(2024)
        syklus.periode(1).tilOgMed shouldBe 14.januar(2024)

        syklus.periode(2).fraOgMed shouldBe 15.januar(2024)
        syklus.periode(2).tilOgMed shouldBe 28.januar(2024)

        syklus.periode(3).fraOgMed shouldBe 29.januar(2024)
        syklus.periode(3).tilOgMed shouldBe 11.februar(2024)
    }

    @Test
    fun `finner meldesyklus for en mindre vakker dag`() {
        val syklus = Meldesyklus(1.juni(2024))

        syklus.periode(1).fraOgMed shouldBe 27.mai(2024)
        syklus.periode(1).tilOgMed shouldBe 9.juni(2024)

        syklus.periode(2).fraOgMed shouldBe 10.juni(2024)
        syklus.periode(2).tilOgMed shouldBe 23.juni(2024)

        syklus.periode(3).fraOgMed shouldBe 24.juni(2024)
        syklus.periode(3).tilOgMed shouldBe 7.juli(2024)
    }
}
