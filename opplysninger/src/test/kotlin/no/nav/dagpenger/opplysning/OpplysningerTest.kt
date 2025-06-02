package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.dagpenger.opplysning.Opplysning.Companion.utenErstattet
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltall
import no.nav.dagpenger.opplysning.TestOpplysningstyper.foreldrevilkår
import no.nav.dagpenger.opplysning.TestOpplysningstyper.undervilkår1
import no.nav.dagpenger.opplysning.TestOpplysningstyper.undervilkår2
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpplysningerTest {
    @Test
    fun `vilkår er avhengig av andre vilkår`() {
        val opplysninger =
            Opplysninger().also {
                Regelkjøring(1.mai, it)
            }

        opplysninger.leggTil(Faktum(undervilkår1, true))
        opplysninger.leggTil(Faktum(undervilkår2, true))
        opplysninger.leggTil(Faktum(foreldrevilkår, true))

        assertTrue(opplysninger.har(undervilkår1))
        assertTrue(opplysninger.har(undervilkår2))
        assertTrue(opplysninger.har(foreldrevilkår))
    }

    @Test
    fun `caching av opplysninger oppdateres`() {
        val opplysninger = Opplysninger()
        opplysninger.finnAlle().shouldBeEmpty()

        val opplysning = Faktum(desimaltall, 0.5)
        opplysninger.leggTil(opplysning)

        opplysninger.finnAlle().shouldContainExactly(opplysning)
    }

    @Test
    fun `skal erstatte opplysning med evig gyldighetsperiode`() {
        val opplysninger1 = Opplysninger()

        opplysninger1.leggTil(Faktum(desimaltall, 0.5))

        val opplysninger2 = Opplysninger(opplysninger1)
        opplysninger2.leggTil(Faktum(desimaltall, 1.0, gyldighetsperiode = Gyldighetsperiode(5.januar, 10.januar)))

        opplysninger2.finnAlle() shouldHaveSize 3
        opplysninger2.finnAlle().utenErstattet() shouldHaveSize 2
    }
}
