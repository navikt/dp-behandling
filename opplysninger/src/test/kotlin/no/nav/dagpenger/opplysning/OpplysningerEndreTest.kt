package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.TestOpplysningstyper.b
import no.nav.dagpenger.opplysning.TestOpplysningstyper.desimaltall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentes
import org.junit.jupiter.api.Test

class OpplysningerEndreTest {
    @Test
    fun `endring av periode erstatter hele opplysningen, uten rester til venstre eller høyre`() {
        val opplysninger = Opplysninger()
        val opprinnelig = Faktum(a, true, Gyldighetsperiode(1.januar))
        opplysninger.leggTil(opprinnelig)

        opplysninger.endre(opprinnelig, gyldighetsperiode = Gyldighetsperiode(10.januar))

        opplysninger.finnAlle(a).map { it.verdi to it.gyldighetsperiode } shouldBe listOf(true to Gyldighetsperiode(10.januar))
        opplysninger.har(a, 5.januar) shouldBe false
    }

    @Test
    fun `endring av verdi beholder identiteten`() {
        val opplysninger = Opplysninger()
        val opprinnelig = Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 20.januar))
        opplysninger.leggTil(opprinnelig)

        val endret = opplysninger.endre(opprinnelig, verdi = 2.0)

        (endret.id == opprinnelig.id) shouldBe false
        endret.identitetId shouldBe opprinnelig.identitetId
        endret.erstatter shouldBe opprinnelig
        opplysninger.finnAlle(desimaltall).map { it.verdi } shouldBe listOf(2.0)
    }

    @Test
    fun `flere endringer etter hverandre gir bare siste revisjon`() {
        val opplysninger = Opplysninger()
        val opprinnelig = Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 20.januar))
        opplysninger.leggTil(opprinnelig)

        val førsteEndring = opplysninger.endre(opprinnelig, verdi = 2.0)
        opplysninger.endre(førsteEndring, verdi = 3.0, gyldighetsperiode = Gyldighetsperiode(5.januar, 20.januar))

        opplysninger.finnAlle(desimaltall).map { it.verdi to it.gyldighetsperiode } shouldBe
            listOf(3.0 to Gyldighetsperiode(5.januar, 20.januar))
        opplysninger.rå() shouldHaveSize 3
    }

    @Test
    fun `endring påvirker ikke andre opplysninger av samme type`() {
        val opplysninger = Opplysninger()
        val første = Faktum(desimaltall, 1.0, Gyldighetsperiode(1.januar, 9.januar))
        val andre = Faktum(desimaltall, 2.0, Gyldighetsperiode(10.januar, 20.januar))
        opplysninger.leggTil(første)
        opplysninger.leggTil(andre)

        opplysninger.endre(andre, verdi = 3.0)

        opplysninger.finnAlle(desimaltall).map { it.verdi } shouldBe listOf(1.0, 3.0)
    }

    @Test
    fun `en revurdering kan endre en arvet opplysning`() {
        val arvet = Faktum(a, true, Gyldighetsperiode(1.januar))
        val revurdering = Opplysninger.basertPå(Opplysninger.med(arvet))

        revurdering.endre(arvet, gyldighetsperiode = Gyldighetsperiode(10.januar))

        revurdering.finnAlle(a).map { it.gyldighetsperiode } shouldBe listOf(Gyldighetsperiode(10.januar))
    }

    @Test
    fun `endring gjør utledede produkter utdaterte slik at de utledes på nytt`() {
        val regelsett =
            vilkår("regelsett") {
                regel(a) { innhentes }
                regel(b) { enAv(a) }
            }
        val opplysninger = Opplysninger()
        val inn = Faktum(a, true, Gyldighetsperiode(1.januar))
        opplysninger.leggTil(inn)
        val regelkjøring = Regelkjøring(1.januar, opplysninger, regelsett)
        regelkjøring.evaluer()
        opplysninger.finnOpplysning(b).verdi shouldBe true

        opplysninger.endre(inn, verdi = false)
        regelkjøring.evaluer()

        opplysninger.finnOpplysning(b).verdi shouldBe false
    }
}
