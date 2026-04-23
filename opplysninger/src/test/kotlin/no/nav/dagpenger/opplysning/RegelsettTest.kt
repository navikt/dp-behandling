package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.beløpA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.beløpB
import no.nav.dagpenger.opplysning.TestOpplysningstyper.faktorA
import no.nav.dagpenger.opplysning.TestOpplysningstyper.faktorB
import no.nav.dagpenger.opplysning.TestOpplysningstyper.grunntall
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.verdier.Beløp
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Testene har feil i assertene sine, de sjekker ikke riktig at behovene ikke lenger er nødvendige")
class RegelsettTest {
    private val regelsett
        get() =
            vilkår(tomHjemmel("regelsett")) {
                regel(grunntall) { innhentes }
                regel(faktorA) { innhentes }
                regel(faktorB) { innhentes }
                regel(beløpA, 1.januar) { multiplikasjon(grunntall, faktorA) }
                regel(beløpA, 1.juni) { multiplikasjon(grunntall, faktorB) }
            }

    @Test
    fun `skal si avhengigheter og produserer`() {
        val regelsett =
            vilkår(tomHjemmel("regelsett")) {
                regel(faktorA) { innhentes }
                regel(faktorB) { innhentes }
                regel(beløpA, 1.januar) { multiplikasjon(grunntall, faktorA) }
                regel(beløpA, 1.juni) { multiplikasjon(grunntall, faktorB) }
                regel(beløpB, 1.juni) { multiplikasjon(grunntall, faktorA) }
            }
        regelsett.produserer.shouldContainExactly(faktorA, faktorB, beløpA, beløpB)
        regelsett.avhengerAv.shouldContainExactly(grunntall)

        regelsett.produserer.contains(beløpA) shouldBe true
        regelsett.avhengerAv.contains(grunntall) shouldBe true

        regelsett.produserer.contains(grunntall) shouldBe false
        regelsett.avhengerAv.contains(beløpA) shouldBe false
    }

    @Test
    fun `regelkjøring i januar skal bruke regler for januar`() {
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(10.januar, opplysninger, regelsett)

        regelkjøring.evaluer().mangler shouldHaveSize 3
        opplysninger.leggTil(Faktum(grunntall, Beløp(3.0))).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(faktorA, 1.0)).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(faktorB, 1.0)).also { regelkjøring.evaluer() }
        regelkjøring.evaluer().informasjonsbehov.keys shouldHaveSize 0
        opplysninger.finnOpplysning(beløpA).verdi shouldBe Beløp(3.0)
    }

    @Test
    fun `regelkjøring i juni skal bruke regler for juni`() {
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(10.juni, opplysninger, regelsett)

        regelkjøring.evaluer().mangler.shouldContainExactly(grunntall, faktorA, faktorB)
        opplysninger.leggTil(Faktum(grunntall, Beløp(3.0))).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(faktorA, 1.0)).also { regelkjøring.evaluer() }
        opplysninger.finnOpplysning(beløpA).verdi shouldBe Beløp(6.0)
    }
}
