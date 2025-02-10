package no.nav.dagpenger.opplysning.regel

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.mai
import no.nav.dagpenger.uuid.UUIDv7
import kotlin.test.Test

class HøyesteAvTest {
    private companion object {
        private val opplysning1 = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "opplysning1")
        private val opplysning2 = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "opplysning2")
        private val høyeste = Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "høyeste")
    }

    @Test
    fun `høyeste av`() {
        val regelsett =
            vilkår("Test") {
                regel(opplysning1) { innhentes }
                regel(opplysning2) { innhentes }
                regel(høyeste) { høyeste.høyesteAv(opplysning1, opplysning2) }
            }
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(23.mai(2024), opplysninger, regelsett)

        opplysninger.leggTil(Faktum(opplysning1, 1)).also { regelkjøring.evaluer() }
        opplysninger.leggTil(Faktum(opplysning2, 2)).also { regelkjøring.evaluer() }
        opplysninger.har(høyeste) shouldBe true
        opplysninger.finnOpplysning(høyeste).verdi shouldBe 2
    }
}
