package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.Utestengning

class UtestengningSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(Utestengning.regelsett)
    private val opplysninger = Opplysninger()

    @BeforeStep
    fun kjørRegler() {
        Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker har søkt om dagpenger") {
            opplysninger.leggTil(Faktum(Søknadstidspunkt.søknadstidspunkt, fraDato))
        }
        Gitt("saksbehandler vurderer at søker er {boolsk}") { utestengt: Boolean ->
            opplysninger.leggTil(Faktum(Utestengning.utestengt, utestengt))
        }
        Så("skal kravet om utestengning være {boolsk}") { oppfylt: Boolean ->
            val faktum = opplysninger.finnOpplysning(Utestengning.ikkeUtestengt)
            faktum.verdi shouldBe oppfylt
        }
    }
}