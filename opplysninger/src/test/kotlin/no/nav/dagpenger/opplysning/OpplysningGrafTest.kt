package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.erSann
import no.nav.dagpenger.opplysning.regel.innhentes
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpplysningGrafTest {
    @Test
    fun `skal finne ubrukte opplysninger`() {
        val a = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "a")
        val b = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "b")
        val c = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "c")
        val d = Opplysningstype.boolsk(Opplysningstype.Id(UUIDv7.ny(), Boolsk), "d")
        val regelsett =
            vilkår("test av regelsett") {
                regel(a) { innhentes }
                regel(b) { innhentes }
                regel(c) { erSann(b) }
            }
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(LocalDate.now(), opplysninger, regelsett)

        val faktumA = Faktum(a, true)
        opplysninger.leggTil(faktumA)
        val faktumB = Faktum(b, true)
        opplysninger.leggTil(faktumB)
        opplysninger.leggTil(Faktum(d, true))

        regelkjøring.evaluer()

        opplysninger.har(c) shouldBe true

        val graf =
            OpplysningGraf(
                opplysninger.aktiveOpplysningerListe,
            )
        graf.hentAlleUtledetAv(faktumB).map { it.opplysningstype } shouldBe listOf(c)
    }
}
