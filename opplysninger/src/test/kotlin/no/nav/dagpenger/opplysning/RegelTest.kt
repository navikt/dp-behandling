package no.nav.dagpenger.opplysning

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskB
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskC
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.alle
import no.nav.dagpenger.opplysning.regel.innhentes
import kotlin.test.Test

class RegelTest {
    @Test
    fun `Regel produserer bare nye opplysningen hvis det den avhenger av er nyere enn det den har produsert`() {
        val regelsett =
            vilkår("regelsett") {
                regel(boolskA) { innhentes }
                regel(boolskB) { innhentes }
                regel(boolskC) {
                    alle(boolskA, boolskB)
                }
            }

        val opplysninger = Opplysninger()
        val regelkjøring1 = Regelkjøring(1.mai, opplysninger, regelsett)
        val opplysning = Faktum(boolskA, true, Gyldighetsperiode(1.januar, 2.mai))
        opplysninger.leggTil(opplysning)
        opplysninger.leggTil(Faktum(boolskB, true))
        regelkjøring1.evaluer()
        opplysninger.finnOpplysning(boolskC).verdi shouldBe true

        // Første forsøk på å fastsette A var feil.
        val regelkjøring2 = Regelkjøring(1.mai, opplysninger, regelsett)
        opplysninger.leggTil(Faktum(boolskA, false, Gyldighetsperiode(1.januar, 2.mai)))
        shouldNotThrow<IllegalArgumentException> { regelkjøring2.evaluer() }

        // Endring av A fører til at C blir beregnet på nytt
        opplysninger.finnOpplysning(boolskC).verdi shouldBe false
    }
}
