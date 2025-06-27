package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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

        // Endre opplysning fra forrige behandling
        opplysninger2.leggTil(Faktum(desimaltall, 1.0, gyldighetsperiode = Gyldighetsperiode(5.januar, 10.januar)))

        opplysninger2.finnAlle() shouldHaveSize 3
        opplysninger2.finnAlle().utenErstattet() shouldHaveSize 2

        // Angre forrige endring, men dette fører til hull
        opplysninger2.leggTil(Faktum(desimaltall, 2.0, gyldighetsperiode = Gyldighetsperiode(8.januar, 10.januar)))

        // Vi har ett hull
        opplysninger2.forDato(6.januar).har(desimaltall) shouldBe false

        // Tett hullet
        opplysninger2.leggTil(Faktum(desimaltall, 3.0, gyldighetsperiode = Gyldighetsperiode(5.januar, 7.januar)))

        // Endre opplysninger i etterkant
        opplysninger2.leggTil(Faktum(desimaltall, 4.0, gyldighetsperiode = Gyldighetsperiode(11.januar, 17.januar)))
        opplysninger2.leggTil(Faktum(desimaltall, 5.0, gyldighetsperiode = Gyldighetsperiode(11.januar, 17.januar)))

        opplysninger2.forDato(1.januar).finnOpplysning(desimaltall).verdi shouldBe 0.5
        opplysninger2.forDato(6.januar).finnOpplysning(desimaltall).verdi shouldBe 3.0
        opplysninger2.forDato(9.januar).finnOpplysning(desimaltall).verdi shouldBe 2.0
        opplysninger2.forDato(12.januar).finnOpplysning(desimaltall).verdi shouldBe 5.0
    }

    @Test
    fun `skal ikke erstatte opplysning med fra og med dato `() {
        val opplysninger1 = Opplysninger()

        opplysninger1.leggTil(Faktum(desimaltall, 0.5, Gyldighetsperiode(5.januar)))

        val opplysninger2 = Opplysninger(opplysninger1)
        opplysninger2.leggTil(Faktum(desimaltall, 1.0, gyldighetsperiode = Gyldighetsperiode(15.januar)))

        opplysninger2.finnAlle() shouldHaveSize 3
    }

    //language=Mermaid
    val blurp =
        """
        ---
        displayMode: compact
        ---
        gantt
            title A Gantt Diagram
            dateFormat YYYY-MM-DD
            section Start
                0.5: 2014-01-01, 2014-01-18
            section Endring 1
                0.5: 2014-01-01, 2014-01-05
                1.0: active, 2014-01-05, 2014-01-10
            section Endring 2
                0.5: 2014-01-01, 2014-01-05
                HØL: crit, 2014-01-05, 2014-01-08
                2.0: active, 2014-01-08, 2014-01-10
            section Endring 3
                0.5: 2014-01-01, 2014-01-05
                3.0: active, 2014-01-05, 2014-01-08
                2.0: 2014-01-08, 2014-01-10
            section Endring 4
                0.5: 2014-01-01, 2014-01-05
                3.0: 2014-01-05, 2014-01-08
                2.0: 2014-01-08, 2014-01-10
                4.0: active, 2014-01-10, 2014-01-17
            section Endring 5
                0.5: 2014-01-01, 2014-01-05
                3.0: 2014-01-05, 2014-01-08
                2.0: 2014-01-08, 2014-01-10
                5.0: active, 2014-01-10, 2014-01-17
        """.trimIndent()
}
