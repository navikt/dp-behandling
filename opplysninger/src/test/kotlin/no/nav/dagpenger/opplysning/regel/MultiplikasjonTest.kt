package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.mai
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MultiplikasjonTest {
    private val sum =
        Opplysningstype.somDesimaltall("SUM")
    private val a =
        Opplysningstype.somDesimaltall("A")
    private val b =
        Opplysningstype.somDesimaltall("B")

    private val opplysninger = Opplysninger()
    private val regelkjøring =
        Regelkjøring(
            1.mai,
            opplysninger,
            Regelsett("regelsett") {
                regel(sum) { multiplikasjon(a, b) }
            },
        )

    @Test
    fun `multiplikasjon regel`() {
        opplysninger.leggTil(Faktum(a, 2.0))
        opplysninger.leggTil(Faktum(b, 2.0))
        val utledet = opplysninger.finnOpplysning(sum)
        assertEquals(4.0, utledet.verdi)
    }
}
