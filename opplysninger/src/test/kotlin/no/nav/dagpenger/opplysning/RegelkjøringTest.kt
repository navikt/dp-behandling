package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.TestOpplysningstyper.b
import no.nav.dagpenger.opplysning.TestOpplysningstyper.c
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.minstAv
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RegelkjøringTest {
    @Test
    fun `Regelsett kan ikke inneholder flere regler som produserer samme opplysningstype`() {
        val regelsett1 =
            vilkår("regelsett") {
                regel(a) { enAv(b) }
            }
        val regelsett2 =
            vilkår("regelsett") {
                regel(a) { enAv(c) }
            }

        assertThrows<IllegalArgumentException> {
            Regelkjøring(1.mai, Opplysninger(), regelsett1, regelsett2)
        }
    }

    @Test
    fun `regelkjøring håndterer nye regler og opplysninger i påbegynte behandlinger`() {
        val opplysningA = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "opplysningA")
        val opplysningB = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "opplysningB")
        val opplysningC = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "opplysningC")
        val opplysningD = Opplysningstype.desimaltall(Opplysningstype.Id(UUIDv7.ny(), Desimaltall), "opplysningD")

        val opplysninger = Opplysninger()
        val regelsett1 =
            vilkår("regelsett") {
                regel(opplysningB) { innhentes }
                regel(opplysningC) { innhentes }
                regel(opplysningA) { minstAv(opplysningC, opplysningB) }
            }
        opplysninger.leggTil(Faktum(opplysningB, 1.5))
        opplysninger.leggTil(Faktum(opplysningC, 0.5))

        // Kjør originale regler
        Regelkjøring(LocalDate.now(), opplysninger, regelsett1).apply {
            val rapport = evaluer()

            rapport.mangler.shouldBeEmpty()
            rapport.kjørteRegler.shouldNotBeEmpty()

            opplysninger.finnOpplysning(opplysningB).verdi shouldBe 1.5
            opplysninger.finnOpplysning(opplysningA).verdi shouldBe 0.5
        }

        // Endring i reglene
        var regelA: Regel<*>? = null
        val regelsett2 =
            vilkår("regelsett") {
                regel(opplysningB) { innhentes }
                regel(opplysningC) { innhentes }
                regel(opplysningD) { innhentes }
                regel(opplysningA) { minstAv(opplysningD, opplysningB).also { regelA = it } }
            }
        opplysninger.leggTil(Faktum(opplysningD, 0.1))

        // Kjør på nytt med endringer i regler
        Regelkjøring(LocalDate.now(), opplysninger, regelsett2).apply {
            val rapport = evaluer()

            rapport.kjørteRegler.shouldContainExactly(regelA)

            opplysninger.finnOpplysning(opplysningB).verdi shouldBe 1.5
            opplysninger.finnOpplysning(opplysningA).verdi shouldBe 0.1
            opplysninger.finnOpplysning(opplysningD).verdi shouldBe 0.1
        }
    }

    @Test
    fun `kan iterere over en periode`() {
        val periode = Regelkjøring.Periode(1.januar, 3.januar)

        periode shouldHaveSize 3

        periode shouldContainExactly listOf(1.januar, 2.januar, 3.januar)
    }

    @Test
    fun `en periode med samme fom og tom skal bare være en dag`() {
        val periode = Regelkjøring.Periode(1.januar, 1.januar)

        periode shouldHaveSize 1

        periode shouldContainExactly listOf(1.januar)
    }

    @Test
    fun `vilkår arver gyldighetsperiode fra opplysninger`() {
        val opplysninger1 = Opplysninger()
        val regelsett =
            vilkår("regelsett") {
                regel(b) { innhentes }
                regel(a) { enAv(b) }
            }

        opplysninger1.leggTil(Faktum(b, true, gyldighetsperiode = Gyldighetsperiode(1.januar))).also {
            Regelkjøring(
                1.januar,
                opplysninger1,
                regelsett,
            ).evaluer()
        }

        opplysninger1.finnOpplysning(a).verdi shouldBe true

        val opplysninger2 = Opplysninger.basertPå(opplysninger1)
        opplysninger2.leggTil(Faktum(b, false, gyldighetsperiode = Gyldighetsperiode(14.januar))).also {
            Regelkjøring(
                15.januar,
                opplysninger2,
                regelsett,
            ).evaluer()
        }

        opplysninger2
            .forDato(15.januar)
            .finnOpplysning(a)
            .verdi shouldBe false
    }
}
