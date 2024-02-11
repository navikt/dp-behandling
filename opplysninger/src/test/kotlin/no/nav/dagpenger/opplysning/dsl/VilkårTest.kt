package no.nav.dagpenger.opplysning.dsl

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.januar
import no.nav.dagpenger.opplysning.mai
import no.nav.dagpenger.opplysning.mars
import org.junit.jupiter.api.Test

class VilkårTest {
    /*
    private val aldersvilkår =
        Vilkår("Test") {
            val virkningsdato = opplysning<LocalDate>("Virkningsdato")
            val fødselsdato = opplysning<LocalDate>("Fødselsdato")
            val aldersgrense =
                opplysning<Int>("Aldersgrense")
                    .regel {
                        oppslag(virkningsdato) { 67 }
                    }
            val datoSøkerNårMaksAlder =
                opplysning<LocalDate>("Dato søker når maks alder")
                    .regel {
                        leggTilÅr(fødselsdato, aldersgrense)
                    }
            val sisteMuligeDagBrukerOppfyllerAlderskrav =
                opplysning<LocalDate>("Siste mulige dag bruker kan oppfylle alderskrav")
                    .regel {
                        sisteDagIMåned(datoSøkerNårMaksAlder)
                    }
            vilkår()
                .regel {
                    førEllerLik(virkningsdato, sisteMuligeDagBrukerOppfyllerAlderskrav)
                }
        }
     */

    @Test
    fun `Gitt at søkeren ikke er 67 år innvilges vilkåret`() {
        val fraDato = 10.mai.atStartOfDay()
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(fraDato, opplysninger, Aldersvilkår.regler())

        opplysninger.leggTil(Faktum(Aldersvilkår.virkningsdato, 2.januar(2024)))
        opplysninger.leggTil(Faktum(Aldersvilkår.fødselsdato, 12.mars(1970)))

        Aldersvilkår.opplysningstyper().size shouldBe 6
        opplysninger.finnOpplysning(Aldersvilkår.vilkår).verdi shouldBe true
    }

    @Test
    fun `Gitt at søkeren er 67 år avslåes vilkåret`() {
        val fraDato = 10.mai.atStartOfDay()
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(fraDato, opplysninger, Aldersvilkår.regler())

        opplysninger.leggTil(Faktum(Aldersvilkår.virkningsdato, 2.januar(2024)))
        opplysninger.leggTil(Faktum(Aldersvilkår.fødselsdato, 12.mars(1951)))

        Aldersvilkår.opplysningstyper().size shouldBe 6
        opplysninger.finnOpplysning(Aldersvilkår.vilkår).verdi shouldBe false
    }
}
