package no.nav.dagpenger.opplysning.dsl

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Vilkår
import no.nav.dagpenger.opplysning.mai
import no.nav.dagpenger.opplysning.regel.dato.førEllerLik
import no.nav.dagpenger.opplysning.regel.dato.leggTilÅr
import no.nav.dagpenger.opplysning.regel.dato.sisteDagIMåned
import no.nav.dagpenger.opplysning.regel.oppslag
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårTest {
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
    @Test
    fun `Alt virker fint`() {

        val fraDato = 10.mai.atStartOfDay()
        val opplysninger = Opplysninger()
        val regelkjøring = Regelkjøring(fraDato, opplysninger, aldersvilkår.regler())

        opplysninger.leggTil(Faktum(aldersvilkår.hentOpplysningstype<LocalDate>("Virkningsdato"), LocalDate.now()))
        opplysninger.leggTil(Faktum(aldersvilkår.hentOpplysningstype<LocalDate>("Fødselsdato"), 20.mai(1981)))

        aldersvilkår shouldNotBe null
        aldersvilkår.opplysningstyper().size shouldBe 6
        opplysninger.finnOpplysning(aldersvilkår.vilkår()).verdi shouldBe true
    }
}
