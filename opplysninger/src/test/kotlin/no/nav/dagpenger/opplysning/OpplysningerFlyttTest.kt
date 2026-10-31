package no.nav.dagpenger.opplysning

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.opplysning.TestOpplysningstyper.a
import no.nav.dagpenger.opplysning.TestOpplysningstyper.b
import no.nav.dagpenger.opplysning.TestOpplysningstyper.c
import org.junit.jupiter.api.Test

class OpplysningerFlyttTest {
    @Test
    fun `kopier har ny id og ingen kobling til gammel kjede`() {
        val gammel = Opplysninger.med(Faktum(a, true))
        val egne = Opplysninger().baserPå(gammel)
        val revisjon = egne.endre(gammel.finnAlle(a).single(), false)

        val kopi = egne.egneForFlytting().single()

        kopi.id shouldNotBe revisjon.id
        kopi.verdi shouldBe false
        kopi.erstatter shouldBe null
        kopi.identitetId shouldBe kopi.id
        revisjon.erstatter shouldNotBe null
    }

    @Test
    fun `interne koblinger peker på kopiene`() {
        val grunnlag = Faktum(a, true)
        val utledet = Faktum(b, true, utledetAv = Utledning("regel", listOf(grunnlag)))

        val kopier = Opplysninger.med(grunnlag, utledet).egneForFlytting()

        kopier shouldHaveSize 2
        val kopiAvGrunnlag = kopier.single { it.opplysningstype == a }
        val kopiAvUtledet = kopier.single { it.opplysningstype == b }
        kopiAvUtledet.utledetAv!!.opplysninger.single() shouldBe kopiAvGrunnlag
        kopiAvGrunnlag.id shouldNotBe grunnlag.id
    }

    @Test
    fun `opplysninger som er utledet av arvede opplysninger droppes transitivt`() {
        val gammel = Opplysninger.med(Faktum(a, true))
        val egne = Opplysninger().baserPå(gammel)
        val arvet = gammel.finnAlle(a).single()
        val utledet = Faktum(b, true, utledetAv = Utledning("regel", listOf(arvet)))
        val videreUtledet = Faktum(c, true, utledetAv = Utledning("regel", listOf(utledet)))
        val uavhengig = Faktum(c, false)
        egne.leggTil(utledet)
        egne.leggTil(videreUtledet)
        egne.leggTil(uavhengig)

        val kopier = egne.egneForFlytting()

        kopier.single().verdi shouldBe false
    }
}
