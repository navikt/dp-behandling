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
}
