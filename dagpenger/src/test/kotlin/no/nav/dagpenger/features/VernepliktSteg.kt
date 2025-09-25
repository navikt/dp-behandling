package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt

class VernepliktSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(Verneplikt.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker har {boolsk} om dagpenger under verneplikt") { søkt: Boolean ->
            opplysninger.leggTil(Faktum<Boolean>(Verneplikt.avtjentVerneplikt, søkt) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Gitt("saksbehandler vurderer at søker har {boolsk} kravet til verneplikt") { oppfyller: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Verneplikt.oppfyllerKravetTilVerneplikt, oppfyller) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Så("skal søker få {boolsk} av verneplikt") { utfall: Boolean ->
            val faktum = opplysninger.finnOpplysning(Verneplikt.oppfyllerKravetTilVerneplikt)
            faktum.verdi shouldBe utfall
        }
    }
}
