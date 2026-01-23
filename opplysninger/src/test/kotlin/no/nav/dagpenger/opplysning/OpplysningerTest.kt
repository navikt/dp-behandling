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

    @Test
    fun `rekkefølge og styr`() {
        val list = listOf("A1", "B1", "A2", "A3")
        val result = list.distinctByLast { it[0] } // selector returnerer første bokstav
        result shouldContainInOrder listOf("B1", "A3")
    }

    @Test
    fun `håndterer overlappende perioder hvor nyeste opplysning har tidligere fraOgMed enn eldste`() {
        // Dette scenarioet reproduserer feilen fra stacktracen prod:
        // - Opplysning med nyere ID (senere opprettet) har fraOgMed=2025-12-29
        // - Opplysning med eldre ID har gyldighetsperiode som slutter 2025-12-11
        // - Når sortert etter ID kommer nyeste først, men logisk sett skal den med tidligste fraOgMed behandles først

        /*

            En opplysning har denne historikken:
            01.12.2025	28.12.2025 --	Ja
            29.12.2025	05.01.2026 --	Nei
            06.01.2026	--	Ja
            Så legger vi til en opplysning som endrer den første perioden til å starte tidligere:
            Og endrer perioden til: 04.12.2025	28.12.2025	-- Nei.

         */
        val opplysninger1 = Opplysninger()

        val første =
            Faktum(
                boolskA,
                true,
                Gyldighetsperiode(
                    fraOgMed = 1 desember 2025,
                    tilOgMed = LocalDate.MAX,
                ),
            )
        opplysninger1.leggTil(første)

        val opplysninger2 = Opplysninger.basertPå(opplysninger1)

        val endring1 =
            Faktum(
                boolskA,
                true,
                Gyldighetsperiode(
                    fraOgMed = 1 desember 2025,
                    tilOgMed = 28 desember 2025,
                ),
            )
        val endring2 =
            Faktum(
                boolskA,
                false,
                Gyldighetsperiode(
                    fraOgMed = 29 desember 2025,
                    tilOgMed = LocalDate.MAX,
                ),
            )

        opplysninger2.leggTil(endring1)
        opplysninger2.leggTil(endring2)

        val opplysninger3 = Opplysninger.basertPå(opplysninger2)
        // Annen opplysning: starter 29. desember (senere dato, men skal ha nyere ID)
        // Dette simulerer at en nyere opplysning legges til som faktisk gjelder en senere periode
        val endring3 =
            Faktum(
                boolskA,
                false,
                Gyldighetsperiode(
                    fraOgMed = 29 desember 2025,
                    tilOgMed = 5 januar 2026,
                ),
            )

        val endring4 =
            Faktum(
                boolskA,
                true,
                Gyldighetsperiode(
                    fraOgMed = 6 januar 2026,
                    tilOgMed = LocalDate.MAX,
                ),
            )
        opplysninger3.leggTil(endring3)
        opplysninger3.leggTil(endring4)

        // Før fiksen ville dette krasje med:
        // java.lang.IllegalArgumentException: fraOgMed=2025-12-29 må være før tilOgMed=2025-12-11
        // fordi lagForkortet fikk opplysningene i feil rekkefølge (sortert etter ID)

        val endring5 =
            Faktum(
                boolskA,
                false,
                Gyldighetsperiode(
                    fraOgMed = 4 desember 2025,
                    tilOgMed = 28 desember 2025,
                ),
            )
        opplysninger3.leggTil(endring5)

        // Etter fiksen skal dette fungere - vi får to separate perioder
        val resultat = opplysninger3.somListe()
        resultat shouldHaveSize 4

        // Verifiser at periodene er korrekte
        val sortert = resultat.sortedBy { it.gyldighetsperiode.fraOgMed }
        sortert[0].gyldighetsperiode.fraOgMed shouldBe 1.desember(2025)
        sortert[0].gyldighetsperiode.tilOgMed shouldBe 3.desember(2025)
        sortert[1].gyldighetsperiode.fraOgMed shouldBe 4.desember(2025)
        sortert[1].gyldighetsperiode.tilOgMed shouldBe 28.desember(2025)
        sortert[2].gyldighetsperiode.fraOgMed shouldBe 29.desember(2025)
        sortert[2].gyldighetsperiode.tilOgMed shouldBe 5.januar(2026)
        sortert[3].gyldighetsperiode.fraOgMed shouldBe 6.januar(2026)
        sortert[3].gyldighetsperiode.tilOgMed shouldBe LocalDate.MAX
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
