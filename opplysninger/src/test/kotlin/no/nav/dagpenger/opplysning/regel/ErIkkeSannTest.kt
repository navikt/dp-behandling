package no.nav.dagpenger.opplysning.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.mai
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ErIkkeSannTest {
    private val opplysning = Opplysningstype.somBoolsk("opplysning")
    private val resultat = Opplysningstype.somBoolsk("resultat")
    private val regelsett =
        Regelsett("Test") {
            regel(resultat) { erIkkeSann(opplysning) }
        }
    private val opplysninger = Opplysninger()

    @BeforeEach
    fun setup() {
        Regelkjøring(23.mai(2024), opplysninger = opplysninger, regelsett)
    }

    @Test
    fun `Opplysning er ikke sann`() {
        opplysninger.leggTil(Faktum(opplysning, false))
        opplysninger.har(resultat) shouldBe true
        opplysninger.finnOpplysning(resultat).verdi shouldBe true
    }

    @Test
    fun `Opplysning er sann`() {
        opplysninger.leggTil(Faktum(opplysning, true))
        opplysninger.har(resultat) shouldBe true
        opplysninger.finnOpplysning(resultat).verdi shouldBe false
    }
}
