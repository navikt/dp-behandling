package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.enAv
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegelkjøringTest {
    @Test
    fun `Regelsett kan ikke inneholder flere regler som produserer samme opplysningstype`() {
        val a = Opplysningstype.somBoolsk("A")
        val regelsett1 =
            Regelsett("regelsett") {
                regel(a) { enAv(Opplysningstype.somBoolsk("B")) }
            }
        val regelsett2 =
            Regelsett("regelsett") {
                regel(a) { enAv(Opplysningstype.somBoolsk("C")) }
            }

        assertThrows<IllegalArgumentException> {
            Regelkjøring(1.mai, Opplysninger(), regelsett1, regelsett2)
        }
    }
}
