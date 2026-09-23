package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.januar
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.regelsett.vilkår.Etablering
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype

class EtableringSteg : No {
    private val fraDato = 1.januar(2025)
    private val regelsett = listOf(Etablering.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at etablering skal vurderes") {
            opplysninger
                .leggTil(Faktum(Rettighetstype.skalEtableringVurderes, true) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Gitt("at etablering ikke skal vurderes") {
            opplysninger
                .leggTil(Faktum(Rettighetstype.skalEtableringVurderes, false) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Og("saksbehandler vurderer at etablering {boolsk} utfallet") { påvirker: Boolean ->
            opplysninger
                .leggTil(Faktum(Etablering.påvirkerUtfallet, påvirker) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Så("skal vilkåret om etablering {boolsk} resultatet") { skalPåvirke: Boolean ->
            Etablering.regelsett.påvirkerResultat(opplysninger) shouldBe skalPåvirke
        }
    }
}
