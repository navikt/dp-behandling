package no.nav.dagpenger.opplysning

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.opplysning.Gyldighetsperiode.Companion.overlappendePerioder
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskB
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class GyldighetsperiodeTest {
    @Test
    fun `likhet test`() {
        val periode1 = Gyldighetsperiode(1.januar, 3.januar)
        val periode2 = Gyldighetsperiode(1.januar, 3.januar)
        val periode3 = Gyldighetsperiode(2.januar, 4.januar)

        periode1 shouldBe periode2
        periode2 shouldBe periode1
        periode3 shouldNotBe periode2
        periode3 shouldNotBe periode1

        periode1.hashCode() shouldBe periode1.hashCode()
        periode1.hashCode() shouldBe periode2.hashCode()
        periode3.hashCode() shouldNotBe periode2.hashCode()
        periode3.hashCode() shouldNotBe periode1.hashCode()

        val perioder = listOf(periode1, periode2, periode3)
        perioder.distinct().size shouldBe 2
    }

    @Test
    fun `kan lage gyldighetsperiode`() {
        val gyldighetsperiode = Gyldighetsperiode(1.januar, 10.januar)

        gyldighetsperiode.contains(1.januar) shouldBe true
        gyldighetsperiode.contains(5.januar) shouldBe true
        gyldighetsperiode.contains(10.januar) shouldBe true
    }

    @Test
    @Disabled
    fun `kan lage gyldige perioder `() {
        shouldThrow<IllegalArgumentException> { Gyldighetsperiode(LocalDate.MAX, LocalDate.MIN) }
        shouldThrow<IllegalArgumentException> { Gyldighetsperiode(2.januar(2024), 1.januar(2024)) }
        shouldNotThrowAny { Gyldighetsperiode(1.januar(2024), 2.januar(2024)) }
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

        a.overlapper(b) shouldBe true
        b.overlapper(a) shouldBe true
    }

    @Test
    fun `overlapper i endepunkt`() {
        val a = Gyldighetsperiode(fraOgMed = 1.januar(2024), tilOgMed = 10.januar(2024))
        val b = Gyldighetsperiode(fraOgMed = 10.januar(2024), tilOgMed = 15.januar(2024))
        a.overlapper(b) shouldBe true
    }

    @Test
    fun `overlapper i starten`() {
        val a = Gyldighetsperiode(fraOgMed = 10.januar(2024), tilOgMed = 20.januar(2024))
        val b = Gyldighetsperiode(fraOgMed = 1.januar(2024), tilOgMed = 15.januar(2024))

        b.overlapper(a) shouldBe true
        a.overlapper(b) shouldBe true
    }

    @Test
    fun `overlapper ikke`() {
        val a = Gyldighetsperiode(1.januar(2024), 10.januar(2024))
        val b = Gyldighetsperiode(11.januar(2024), 20.januar(2024))
        a.overlapper(b) shouldBe false
    }

    @Test
    fun `erFør og erEtter`() {
        val a = Gyldighetsperiode(1.januar(2024), 10.januar(2024))
        val b = Gyldighetsperiode(15.januar(2024), 20.januar(2024))

        a.erFør(b) shouldBe true
        a.erEtter(b) shouldBe false
        b.erFør(a) shouldBe false
        b.erEtter(a) shouldBe true

        // Overlappende perioder er verken før eller etter
        val c = Gyldighetsperiode(5.januar(2024), 15.januar(2024))
        a.erFør(c) shouldBe false
        a.erEtter(c) shouldBe false
    }

    @Test
    fun `tilstøter - kant i kant`() {
        val a = Gyldighetsperiode(1.januar(2024), 10.januar(2024))
        val b = Gyldighetsperiode(11.januar(2024), 20.januar(2024))
        val c = Gyldighetsperiode(12.januar(2024), 20.januar(2024))

        a.tilstøter(b) shouldBe true
        b.tilstøter(a) shouldBe true
        a.tilstøter(c) shouldBe false
    }

    @Test
    fun `tilstøter - med MAX`() {
        val a = Gyldighetsperiode(1.januar(2024))
        val b = Gyldighetsperiode(5.januar(2024), 10.januar(2024))

        // a har tilOgMed=MAX, kan ikke tilstøte noe
        a.tilstøter(b) shouldBe false
    }

    @Test
    fun `overlapp - overlappende perioder`() {
        val a = Gyldighetsperiode(1.januar(2024), 10.januar(2024))
        val b = Gyldighetsperiode(5.januar(2024), 15.januar(2024))

        a.overlapp(b) shouldBe Gyldighetsperiode(5.januar(2024), 10.januar(2024))
        b.overlapp(a) shouldBe Gyldighetsperiode(5.januar(2024), 10.januar(2024))
    }

    @Test
    fun `overlapp - ingen overlapp`() {
        val a = Gyldighetsperiode(1.januar(2024), 10.januar(2024))
        val b = Gyldighetsperiode(15.januar(2024), 20.januar(2024))

        a.overlapp(b) shouldBe null
    }

    @Test
    fun `minus - trekk fra helt overlappende`() {
        val a = Gyldighetsperiode(5.januar(2024), 15.januar(2024))
        val b = Gyldighetsperiode(1.januar(2024), 20.januar(2024))

        (a - b).shouldBeEmpty()
    }

    @Test
    fun `minus - trekk fra delvis overlappende i midten`() {
        val a = Gyldighetsperiode(1.januar(2024), 20.januar(2024))
        val b = Gyldighetsperiode(5.januar(2024), 10.januar(2024))

        val resultat = a - b
        resultat shouldHaveSize 2
        resultat[0] shouldBe Gyldighetsperiode(1.januar(2024), 4.januar(2024))
        resultat[1] shouldBe Gyldighetsperiode(11.januar(2024), 20.januar(2024))
    }

    @Test
    fun `minus - trekk fra overlappende til venstre`() {
        val a = Gyldighetsperiode(5.januar(2024), 20.januar(2024))
        val b = Gyldighetsperiode(1.januar(2024), 10.januar(2024))

        val resultat = a - b
        resultat shouldContainExactly listOf(Gyldighetsperiode(11.januar(2024), 20.januar(2024)))
    }

    @Test
    fun `minus - trekk fra overlappende til høyre`() {
        val a = Gyldighetsperiode(1.januar(2024), 15.januar(2024))
        val b = Gyldighetsperiode(10.januar(2024), 20.januar(2024))

        val resultat = a - b
        resultat shouldContainExactly listOf(Gyldighetsperiode(1.januar(2024), 9.januar(2024)))
    }

    @Test
    fun `minus - ingen overlapp`() {
        val a = Gyldighetsperiode(1.januar(2024), 10.januar(2024))
        val b = Gyldighetsperiode(15.januar(2024), 20.januar(2024))

        (a - b) shouldContainExactly listOf(a)
    }

    @Test
    fun `minus collection - trekk fra flere perioder gir hull`() {
        val hel = Gyldighetsperiode(1.januar(2024), 30.januar(2024))
        val perioder =
            listOf(
                Gyldighetsperiode(1.januar(2024), 10.januar(2024)),
                Gyldighetsperiode(20.januar(2024), 30.januar(2024)),
            )

        val hull = hel.minus(perioder)
        hull shouldContainExactly listOf(Gyldighetsperiode(11.januar(2024), 19.januar(2024)))
    }

    @Test
    fun `minus collection - med MIN og MAX`() {
        val hel = Gyldighetsperiode() // MIN til MAX
        val perioder =
            listOf(
                Gyldighetsperiode(1.januar(2024), 10.januar(2024)),
                Gyldighetsperiode(20.januar(2024), 30.januar(2024)),
            )

        val hull = hel.minus(perioder)
        hull shouldHaveSize 3
        hull[0] shouldBe Gyldighetsperiode(LocalDate.MIN, 31.desember(2023))
        hull[1] shouldBe Gyldighetsperiode(11.januar(2024), 19.januar(2024))
        hull[2] shouldBe Gyldighetsperiode(31.januar(2024), LocalDate.MAX)
    }
}
