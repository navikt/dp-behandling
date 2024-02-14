package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.mai
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SisteDagIMånedEllerLikTest {
    private val vilkår = Opplysningstype.somBoolsk("Vilkår")
    private val a = Opplysningstype.somDesimaltall("A")
    private val b = Opplysningstype.somDesimaltall("B")

    private val opplysninger = Opplysninger()
    private val regelkjøring =
        Regelkjøring(
            1.mai,
            opplysninger,
            Regelsett("regelsett") {
                regel(vilkår) { størreEnnEllerLik(a, b) }
            },
        )

    @Test
    fun `større enn`() {
        opplysninger.leggTil(Faktum(a, 2.0))
        opplysninger.leggTil(Faktum(b, 1.0))
        val utledet = opplysninger.finnOpplysning(vilkår)
        assertTrue(utledet.verdi)
    }

    @Test
    fun `ikke større enn`() {
        opplysninger.leggTil(Faktum(a, 2.0))
        opplysninger.leggTil(Faktum(b, 1.0))
        val utledet = opplysninger.finnOpplysning(vilkår)
        assertTrue(utledet.verdi)
    }
}
