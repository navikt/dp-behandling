package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Gyldighetsperiode.Companion.overlappendePerioder
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskB
import org.junit.jupiter.api.Test

internal class GyldighetsperiodeTest {
    @Test
    fun `kan lage gyldighetsperiode`() {
        val gyldighetsperiode = Gyldighetsperiode(1.januar, 10.januar)

        gyldighetsperiode.contains(1.januar) shouldBe true
        gyldighetsperiode.contains(5.januar) shouldBe true
        gyldighetsperiode.contains(10.januar) shouldBe true
    }

    @Test
    fun `kan sjekke overlapp på flere perioder`() {
        val perioder =
            mapOf(
                boolskA to
                    listOf(
                        Gyldighetsperiode(1.august(2025), 3.august(2025)),
                        Gyldighetsperiode(4.august(2025), 10.august(2025)),
                        Gyldighetsperiode(11.august(2025), 12.august(2025)),
                    ),
                boolskB to
                    listOf(
                        Gyldighetsperiode(1.august(2025), 3.august(2025)),
                        Gyldighetsperiode(7.august(2025), 10.august(2025)),
                        Gyldighetsperiode(11.august(2025), 12.august(2025)),
                        Gyldighetsperiode(5.august(2025), 9.august(2025)),
                    ),
            )

        val overlapp =
            perioder.mapValues { (_, gyldighetsperioder) ->
                gyldighetsperioder.overlappendePerioder()
            }

        overlapp shouldBe mapOf(boolskA to false, boolskB to true)
    }

    @Test
    fun `overlapper inni`() {
        val a = Gyldighetsperiode(fraOgMed = 1.januar(2024), tilOgMed = 10.januar(2024))
        val b = Gyldighetsperiode(fraOgMed = 5.januar(2024), tilOgMed = 20.januar(2024))

        a.overlapp(b) shouldBe true
        b.overlapp(a) shouldBe true
    }

    @Test
    fun `overlapper i endepunkt`() {
        val a = Gyldighetsperiode(fraOgMed = 1.januar(2024), tilOgMed = 10.januar(2024))
        val b = Gyldighetsperiode(fraOgMed = 10.januar(2024), tilOgMed = 15.januar(2024))
        a.overlapp(b) shouldBe true
    }

    @Test
    fun `overlapper i starten`() {
        val a = Gyldighetsperiode(fraOgMed = 10.januar(2024), tilOgMed = 20.januar(2024))
        val b = Gyldighetsperiode(fraOgMed = 1.januar(2024), tilOgMed = 15.januar(2024))

        b.overlapp(a) shouldBe true
        a.overlapp(b) shouldBe true
    }

    @Test
    fun `overlapper ikke`() {
        val a = Gyldighetsperiode(1.januar(2024), 10.januar(2024))
        val b = Gyldighetsperiode(11.januar(2024), 20.januar(2024))
        a.overlapp(b) shouldBe false
    }
}
