package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.TestOpplysningstyper.b
import no.nav.dagpenger.opplysning.TestOpplysningstyper.c
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.minstAv
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RegelkjøringTest {
    @Test
    fun `Regelsett kan ikke inneholder flere regler som produserer samme opplysningstype`() {
        val regelsett1 =
            Regelsett("regelsett") {
                regel(a) { enAv(b) }
            }
        val regelsett2 =
            Regelsett("regelsett") {
                regel(a) { enAv(c) }
            }

        assertThrows<IllegalArgumentException> {
            Regelkjøring(1.mai, Opplysninger(), regelsett1, regelsett2)
        }
    }

    @Test
    fun `regelkjøring håndterer nye regler og opplysninger i påbegynte behandlinger`() {
        val opplysningA = Opplysningstype.somDesimaltall("opplysningA")
        val opplysningB = Opplysningstype.somDesimaltall("opplysningB")
        val opplysningC = Opplysningstype.somDesimaltall("opplysningC")
        val opplysningD = Opplysningstype.somDesimaltall("opplysningD")

        val opplysninger = Opplysninger()
        val regelsett1 =
            Regelsett("regelsett") {
                regel(opplysningB) { innhentes }
                regel(opplysningC) { innhentes }
                regel(opplysningA) { minstAv(opplysningB) }
            }
        opplysninger.leggTil(Faktum(opplysningB, 1.5))
        opplysninger.leggTil(Faktum(opplysningC, 0.5))

        // Kjør originale regler
        Regelkjøring(LocalDate.now(), opplysninger, regelsett1).apply {
            val rapport = evaluer()

            rapport.mangler.shouldBeEmpty()
            rapport.kjørteRegler.shouldNotBeEmpty()

            opplysninger.finnOpplysning(opplysningB).verdi shouldBe 1.5
            opplysninger.finnOpplysning(opplysningA).verdi shouldBe 1.5
        }

        // Endring i reglene
        val regelsett2 =
            Regelsett("regelsett") {
                regel(opplysningB) { innhentes }
                regel(opplysningC) { innhentes }
                regel(opplysningD) { minstAv(opplysningC) }
                regel(opplysningA) { minstAv(opplysningB, opplysningD) }
            }

        // Kjør nye regler med nye opplysninger
        Regelkjøring(LocalDate.now(), opplysninger, regelsett2).apply {
            val rapport = evaluer()

            // rapport.mangler.shouldContain(opplysningD)

            opplysninger.finnOpplysning(opplysningB).verdi shouldBe 1.5
            opplysninger.finnOpplysning(opplysningA).verdi shouldBe 1.5
            opplysninger.finnOpplysning(opplysningD).verdi shouldBe 0.5
        }

        /*
        // Ikke det vi vil teste nå, men burde også fungere
        opplysninger.leggTil(Faktum(opplysningD, 0.5))
        Regelkjøring(LocalDate.now(), opplysninger, regelsett2).apply {
            val rapport = evaluer()

            rapport.mangler.shouldBeEmpty()
            rapport.kjørteRegler.shouldContain(listOf(regelC, regelA))

            opplysninger.finnOpplysning(opplysningC).verdi shouldBe 0.5
        }*/
    }
}
