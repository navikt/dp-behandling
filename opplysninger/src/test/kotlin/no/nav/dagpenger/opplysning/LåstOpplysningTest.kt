package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.addisjon
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LåstOpplysningTest {
    private val a = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "a")
    private val b = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "b")
    private val c = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "c")

    @Test
    fun `låst opplysning skal ikke utledes på nytt når underliggende endres`() {
        val opplysninger = Opplysninger()
        val regelsett =
            vilkår("test") {
                regel(a) { innhentes }
                regel(b) { innhentes }
                regel(c) { addisjon(a, b) }
            }

        opplysninger.leggTil(Faktum(a, Beløp(1.0)))
        opplysninger.leggTil(Faktum(b, Beløp(2.0)))

        val regelkjøring1 = Regelkjøring(LocalDate.now(), opplysninger, regelsett)
        regelkjøring1.evaluer()

        opplysninger.finnOpplysning(c).verdi shouldBe Beløp(3.0)
        val cId = opplysninger.finnOpplysning(c).id

        // Lås opplysning c
        opplysninger.låsOpplysning(cId)

        println("DEBUG: After locking, c.erLåst = ${opplysninger.finnOpplysning(c).erLåst}")
        println("DEBUG: Number of c opplysninger = ${opplysninger.finnAlle(c).size}")

        // Endre underliggende opplysning a
        opplysninger.leggTil(Faktum(a, Beløp(10.0)))

        println("DEBUG: After changing a, number of c opplysninger = ${opplysninger.finnAlle(c).size}")
        println("DEBUG: All c values and locked status: ${opplysninger.finnAlle(c).map { "val=${it.verdi}, locked=${it.erLåst}" }}")

        val regelkjøring2 = Regelkjøring(LocalDate.now(), opplysninger, regelsett)
        val rapport = regelkjøring2.evaluer()

        println("DEBUG: After regelkjøring2, number of c opplysninger = ${opplysninger.finnAlle(c).size}")
        println("DEBUG: All c values and locked status: ${opplysninger.finnAlle(c).map { "val=${it.verdi}, locked=${it.erLåst}" }}")
        println(
            "DEBUG: Kjørte regler = ${rapport.kjørteRegler.size}, ${rapport.kjørteRegler.joinToString {
                it::class.java.simpleName + "for ${it.produserer.navn}"
            }}",
        )

        // c skal fortsatt være 3.0, ikke 12.0, fordi den er låst
        opplysninger.finnOpplysning(c).verdi shouldBe Beløp(3.0)
        opplysninger.finnOpplysning(c).id shouldBe cId
        rapport.kjørteRegler.shouldBeEmpty()
    }

    @Test
    fun `ulåst opplysning skal utledes på nytt når underliggende endres`() {
        val opplysninger = Opplysninger()
        val regelsett =
            vilkår("test") {
                regel(a) { innhentes }
                regel(b) { innhentes }
                regel(c) { addisjon(a, b) }
            }

        opplysninger.leggTil(Faktum(a, Beløp(1.0)))
        opplysninger.leggTil(Faktum(b, Beløp(2.0)))

        val regelkjøring1 = Regelkjøring(LocalDate.now(), opplysninger, regelsett)
        regelkjøring1.evaluer()

        opplysninger.finnOpplysning(c).verdi shouldBe Beløp(3.0)

        // Endre underliggende opplysning a UTEN å låse c
        opplysninger.leggTil(Faktum(a, Beløp(10.0)))

        val regelkjøring2 = Regelkjøring(LocalDate.now(), opplysninger, regelsett)
        val rapport = regelkjøring2.evaluer()

        // c skal være 12.0, fordi den ikke er låst
        opplysninger.finnOpplysning(c).verdi shouldBe Beløp(12.0)
        rapport.kjørteRegler shouldHaveSize 1
    }

    @Test
    fun `kan låse opplysning og alle avledede opplysninger`() {
        val d = Opplysningstype.beløp(Opplysningstype.Id(UUIDv7.ny(), Penger), "d")

        val opplysninger = Opplysninger()
        val regelsett =
            vilkår("test") {
                regel(a) { innhentes }
                regel(b) { innhentes }
                regel(c) { addisjon(a, b) }
                regel(d) { addisjon(c, b) }
            }

        opplysninger.leggTil(Faktum(a, Beløp(1.0)))
        opplysninger.leggTil(Faktum(b, Beløp(2.0)))

        val regelkjøring1 = Regelkjøring(LocalDate.now(), opplysninger, regelsett)
        regelkjøring1.evaluer()

        opplysninger.finnOpplysning(c).verdi shouldBe Beløp(3.0)
        opplysninger.finnOpplysning(d).verdi shouldBe Beløp(5.0)

        // Lås a og alle avledede (c og d)
        opplysninger.låsOpplysningOgAvledede(opplysninger.finnOpplysning(a).id)

        // Endre a
        opplysninger.leggTil(Faktum(a, Beløp(10.0)))

        val regelkjøring2 = Regelkjøring(LocalDate.now(), opplysninger, regelsett)
        val rapport = regelkjøring2.evaluer()

        // Både c og d skal være uendret fordi de er låst
        opplysninger.finnOpplysning(c).verdi shouldBe Beløp(3.0)
        opplysninger.finnOpplysning(d).verdi shouldBe Beløp(5.0)
        rapport.kjørteRegler.shouldBeEmpty()
    }

    @Test
    fun `kan låse opp opplysning`() {
        val opplysninger = Opplysninger()
        val regelsett =
            vilkår("test") {
                regel(a) { innhentes }
                regel(b) { innhentes }
                regel(c) { addisjon(a, b) }
            }

        opplysninger.leggTil(Faktum(a, Beløp(1.0)))
        opplysninger.leggTil(Faktum(b, Beløp(2.0)))

        val regelkjøring1 = Regelkjøring(LocalDate.now(), opplysninger, regelsett)
        regelkjøring1.evaluer()

        opplysninger.finnOpplysning(c).verdi shouldBe Beløp(3.0)
        val cId = opplysninger.finnOpplysning(c).id

        // Lås og lås opp igjen
        opplysninger.låsOpplysning(cId)
        opplysninger.låsOpp(cId)

        // Endre underliggende opplysning a
        opplysninger.leggTil(Faktum(a, Beløp(10.0)))

        val regelkjøring2 = Regelkjøring(LocalDate.now(), opplysninger, regelsett)
        val rapport = regelkjøring2.evaluer()

        // c skal være 12.0, fordi den er låst opp
        opplysninger.finnOpplysning(c).verdi shouldBe Beløp(12.0)
        rapport.kjørteRegler shouldHaveSize 1
    }

    @Test
    fun `erLåst flagg bevares ved lagForkortet`() {
        val opplysning = Faktum(a, Beløp(1.0))
        opplysning.lås()
        opplysning.erLåst shouldBe true

        val annenOpplysning = Faktum(a, Beløp(2.0), gyldighetsperiode = Gyldighetsperiode(fom = LocalDate.now().plusDays(1)))
        val forkortet = opplysning.lagForkortet(annenOpplysning)

        forkortet.erLåst shouldBe true
    }
}
