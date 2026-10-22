package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltall
import org.junit.jupiter.api.Test

class TombstoneTest {
    @Test
    fun `tombstone skjuler opplysningen i hele sin periode uten å slette noe`() {
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 20.januar)))

        opplysninger.tombstone(desimaltall)

        opplysninger.har(desimaltall) shouldBe false
        opplysninger.finnAlle(desimaltall) shouldHaveSize 0
        opplysninger.rå() shouldHaveSize 2
        opplysninger.fjernet() shouldHaveSize 0
    }

    @Test
    fun `tombstone for en periode lar resten av opplysningen stå`() {
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 20.januar)))

        opplysninger.tombstone(desimaltall, Gyldighetsperiode(5.januar, 20.januar))

        opplysninger.finnAlle(desimaltall).map { it.gyldighetsperiode } shouldBe listOf(Gyldighetsperiode(1.januar, 4.januar))
        opplysninger.har(desimaltall, 10.januar) shouldBe false
    }

    @Test
    fun `tombstone dekker flere segmenter med hver sin verdi, og høyre rest gjenopptas ikke`() {
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 10.januar)))
        opplysninger.leggTil(Faktum(desimaltall, 2.0, Gyldighetsperiode(11.januar, 20.januar)))

        opplysninger.tombstone(desimaltall, Gyldighetsperiode(5.januar, 15.januar))

        opplysninger.finnAlle(desimaltall).map { it.verdi to it.gyldighetsperiode } shouldBe
            listOf(1.0 to Gyldighetsperiode(1.januar, 4.januar))
        opplysninger.rå().filterIsInstance<Tombstone<*>>() shouldHaveSize 2
    }

    @Test
    fun `ny opplysning etter tombstone vinner over den`() {
        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 20.januar)))
        opplysninger.tombstone(desimaltall)

        opplysninger.leggTil(Faktum(desimaltall, 3.0, Gyldighetsperiode(1.januar, 20.januar)))

        opplysninger.finnOpplysning(desimaltall).verdi shouldBe 3.0
    }

    @Test
    fun `tombstone på arvet opplysning skjuler den i revurderingen, men ikke i grunnlaget`() {
        val grunnlag = Opplysninger()
        grunnlag.leggTil(Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 20.januar)))
        val revurdering = Opplysninger.basertPå(grunnlag)

        revurdering.tombstone(desimaltall)

        revurdering.har(desimaltall) shouldBe false
        grunnlag.har(desimaltall) shouldBe true
    }

    @Test
    fun `tombstone uten noe å skjule gjør ingenting`() {
        val opplysninger = Opplysninger()

        opplysninger.tombstone(desimaltall)

        opplysninger.rå() shouldHaveSize 0
    }
}
