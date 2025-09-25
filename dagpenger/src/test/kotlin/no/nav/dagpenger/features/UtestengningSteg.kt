package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Utestengning
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt.prøvingsdato

class UtestengningSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(Utestengning.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker har søkt om dagpenger") {
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato)).also { regelkjøring.evaluer() }
        }
        Gitt("saksbehandler vurderer at søker er {boolsk}") { utestengt: Boolean ->
            opplysninger.leggTil(Faktum<Boolean>(Utestengning.utestengt, utestengt) as Opplysning<*>).also { regelkjøring.evaluer() }
        }
        Så("skal kravet om utestengning være {boolsk}") { oppfylt: Boolean ->
            val faktum = opplysninger.finnOpplysning(Utestengning.oppfyllerKravetTilIkkeUtestengt)
            faktum.verdi shouldBe oppfylt
        }
    }
}
