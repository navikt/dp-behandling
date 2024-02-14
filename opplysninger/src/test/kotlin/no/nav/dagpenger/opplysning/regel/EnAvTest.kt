package no.nav.dagpenger.opplysning.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.mai
import org.junit.jupiter.api.Test

internal class EnAvTest {
    private val opplysningB = Opplysningstype.somBoolsk("B")
    private val opplysningC = Opplysningstype.somBoolsk("C")
    private val produserer = Opplysningstype.somBoolsk("A")
    private val opplysninger = Opplysninger()
    private val regelkjøring =
        Regelkjøring(
            1.mai,
            opplysninger,
            Regelsett("regelsett") {
                regel(produserer) { enAv(opplysningB, opplysningC) }
            },
        )

    @Test
    fun `hvis en av opplysningene er sanne så er utledningen sann`() {
        opplysninger.leggTil(Faktum(opplysningB, false))
        opplysninger.leggTil(Faktum(opplysningC, true))
        val utledet = opplysninger.finnOpplysning(produserer)
        utledet.verdi shouldBe true
    }

    @Test
    fun `hvis ingen av opplysningene er sanne så er utledningen usann`() {
        opplysninger.leggTil(Faktum(opplysningB, false))
        opplysninger.leggTil(Faktum(opplysningC, false))
        val utledet = opplysninger.finnOpplysning(produserer)
        utledet.verdi shouldBe false
    }

    @Test
    fun `hvis begge opplysningene er sanne så er utledningen sann`() {
        opplysninger.leggTil(Faktum(opplysningB, true))
        opplysninger.leggTil(Faktum(opplysningC, true))
        val utledet = opplysninger.finnOpplysning(produserer)
        utledet.verdi shouldBe true
        utledet.verdi shouldBe true
    }
}
