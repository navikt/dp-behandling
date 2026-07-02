package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato

class SanksjonSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = RegelverkDagpenger.regelsettFor(Sanksjonsperiode.antallSanksjonsdager)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker har søkt om dagpenger for vurdering av sanksjon") {
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato)).also { regelkjøring.evaluer() }
        }
        Og("saksbehandler ilegger sanksjon") {
            opplysninger.leggTil(Faktum(Sanksjonsperiode.harSanksjon, true)).also { regelkjøring.evaluer() }
        }
        Og("saksbehandler ilegger ikke sanksjon") {
            opplysninger.leggTil(Faktum(Sanksjonsperiode.harSanksjon, false)).also { regelkjøring.evaluer() }
        }
        Og("saksbehandler ilegger sanksjon i {string} uker") { uker: String ->
            opplysninger.leggTil(Faktum(Sanksjonsperiode.harSanksjon, true))
            opplysninger.leggTil(Faktum(Sanksjonsperiode.antallSanksjonsuker, uker.toInt())).also { regelkjøring.evaluer() }
        }
        Så("skal antall sanksjonsdager være {string}") { dager: String ->
            opplysninger.finnOpplysning(Sanksjonsperiode.antallSanksjonsdager).verdi shouldBe dager.toInt()
        }
    }
}
