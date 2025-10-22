package no.nav.dagpenger.opplysning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.boolskA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltall
import no.nav.dagpenger.opplysning.TestOpplysningstyper.foreldrevilkår
import no.nav.dagpenger.opplysning.TestOpplysningstyper.undervilkår1
import no.nav.dagpenger.opplysning.TestOpplysningstyper.undervilkår2
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
        opplysninger.somListe().shouldBeEmpty()

        val opplysning = Faktum(desimaltall, 0.5)
        opplysninger.leggTil(opplysning)

        opplysninger.somListe().shouldContainExactly(opplysning)
    }

    @Test
    fun `skal erstatte opplysning med evig gyldighetsperiode`() {
        val opplysninger1 = Opplysninger()

        opplysninger1.leggTil(Faktum(desimaltall, 0.5))

        val opplysninger2 = Opplysninger.basertPå(opplysninger1)

        // Endre opplysning fra forrige behandling
        val forkortetOpplysning = Faktum(desimaltall, 1.0, gyldighetsperiode = Gyldighetsperiode(5.januar, 7.januar))
        opplysninger2.leggTil(forkortetOpplysning)

        opplysninger1.somListe() shouldHaveSize 1
        opplysninger2.somListe() shouldHaveSize 2

        opplysninger2.somListe().map { it.verdi } shouldContainInOrder listOf(0.5, 1.0)

        // Legger til ny opplysning etter
        opplysninger2.leggTil(Faktum(desimaltall, 2.0, gyldighetsperiode = Gyldighetsperiode(8.januar, 10.januar)))
        opplysninger2.somListe().map { it.verdi } shouldContainInOrder listOf(0.5, 1.0, 2.0)

        // Endre opplysninger i etterkant
        opplysninger2.leggTil(Faktum(desimaltall, 4.0, gyldighetsperiode = Gyldighetsperiode(11.januar, 17.januar)))
        opplysninger2.leggTil(Faktum(desimaltall, 5.0, gyldighetsperiode = Gyldighetsperiode(11.januar, 17.januar)))

        opplysninger2.somListe().map { it.verdi } shouldContainInOrder listOf(0.5, 1.0, 2.0, 5.0)

        opplysninger2.forDato(1.januar).finnOpplysning(desimaltall).verdi shouldBe 0.5
        opplysninger2.forDato(9.januar).finnOpplysning(desimaltall).verdi shouldBe 2.0
        opplysninger2.forDato(12.januar).finnOpplysning(desimaltall).verdi shouldBe 5.0
    }

    @Test
    fun `skal ikke erstatte opplysning med fra og med dato `() {
        val opplysninger1 = Opplysninger()

        opplysninger1.leggTil(Faktum(desimaltall, 0.5, Gyldighetsperiode(5.januar)))

        val opplysninger2 = Opplysninger.basertPå(opplysninger1)
        opplysninger2.leggTil(Faktum(desimaltall, 1.0, gyldighetsperiode = Gyldighetsperiode(15.januar)))

        opplysninger2.somListe() shouldHaveSize 2
    }

    @Test
    fun `🍌☎ 🐍🍄🍄`() {
        val opplysninger = Opplysninger()

        opplysninger.leggTil(Faktum(boolskA, true, Gyldighetsperiode()))
        opplysninger.somListe() shouldHaveSize 1
        opplysninger.leggTil(Faktum(boolskA, false, Gyldighetsperiode(2.oktober)))
        opplysninger.somListe() shouldHaveSize 1
        opplysninger.leggTil(Faktum(boolskA, false, Gyldighetsperiode(1.oktober, 2.oktober)))
        opplysninger.somListe() shouldHaveSize 1
        opplysninger.leggTil(Faktum(boolskA, true, Gyldighetsperiode()))

        opplysninger.somListe() shouldHaveSize 1
        opplysninger.fjernet() shouldHaveSize 3
    }

    @Test
    fun `kan ikke slette opplysninger i basert på opplysninger`() {
        val opplysninger1 = Opplysninger()

        opplysninger1.leggTil(Faktum(desimaltall, 0.5))

        val opplysninger2 = Opplysninger.basertPå(opplysninger1)

        // Kan ikke slette opplysning i basert på opplysninger
        shouldThrow<OpplysningIkkeFunnetException> {
            opplysninger2.fjern(opplysninger2.finnOpplysning(desimaltall).id)
        }
    }

    @Test
    fun `lager tidslinje og sånt`() {
        val datoer = listOf(5.januar, 11.januar, 21.januar, 25.januar, 30.januar, 5.juni, 21.juni)

        val sisteOpplysninger =
            datoer.fold(Opplysninger()) { acc, dato ->
                Opplysninger.basertPå(acc).apply {
                    leggTil(Faktum(undervilkår1, true, gyldighetsperiode = Gyldighetsperiode(dato)))
                    leggTil(Faktum(undervilkår2, true, gyldighetsperiode = Gyldighetsperiode(dato)))
                }
            }

        with(sisteOpplysninger.somListe()) {
            this shouldHaveSize datoer.size * 2
            this[0].gyldighetsperiode.fraOgMed shouldBe 5.januar
            this[0].gyldighetsperiode.tilOgMed shouldBe 10.januar

            this[2].gyldighetsperiode.fraOgMed shouldBe 11.januar
            this[2].gyldighetsperiode.tilOgMed shouldBe 20.januar

            this[4].gyldighetsperiode.fraOgMed shouldBe 21.januar
            this[4].gyldighetsperiode.tilOgMed shouldBe 24.januar

            this.last { it.er(undervilkår1) }.gyldighetsperiode.tilOgMed shouldBe LocalDate.MAX
            this.last { it.er(undervilkår2) }.gyldighetsperiode.tilOgMed shouldBe LocalDate.MAX
        }
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
