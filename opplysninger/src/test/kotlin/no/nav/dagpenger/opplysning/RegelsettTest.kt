package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegelsettTest {
    private val grunntall = Opplysningstype.somDesimaltall("A")
    private val faktorA = Opplysningstype.somDesimaltall("FaktorA")
    private val faktorB = Opplysningstype.somDesimaltall("FaktorB")
    private val produserer = Opplysningstype.somDesimaltall("Output")
    private val regelsett
        get() =
            Regelsett("regelsett") {
                regel(produserer, 1.januar) { multiplikasjon(grunntall, faktorA) }
                regel(produserer, 1.juni) { multiplikasjon(grunntall, faktorB) }
            }

    @Test
    fun `regelkjøring i januar skal bruke regler for januar`() {
        with(Opplysninger()) {
            val regelkjøring = Regelkjøring(10.januar, this, regelsett)
            assertEquals(2, regelkjøring.trenger(produserer).size)
            leggTil(Faktum(grunntall, 3.0))
            leggTil(Faktum(faktorA, 1.0))
            finnOpplysning(produserer).verdi shouldBe 3.0
        }
    }

    @Test
    fun `regelkjøring i juni skal bruke regler for juni`() {
        with(Opplysninger()) {
            val regelkjøring = Regelkjøring(10.juni, this, regelsett)
            assertEquals(2, regelkjøring.trenger(produserer).size)
            leggTil(Faktum(grunntall, 3.0))
            leggTil(Faktum(faktorB, 2.0))
            finnOpplysning(produserer).verdi shouldBe 6.0
        }
    }

    @Test
    fun `regelkjøring i juni skal gjenbruke verdi fra januar`() {
        with(Opplysninger(listOf(Faktum(produserer, 3.0)))) {
            val regelkjøring = Regelkjøring(10.juni, this, regelsett)
            assertEquals(0, regelkjøring.trenger(produserer).size)
            finnOpplysning(produserer).verdi shouldBe 3.0
        }
    }
}
