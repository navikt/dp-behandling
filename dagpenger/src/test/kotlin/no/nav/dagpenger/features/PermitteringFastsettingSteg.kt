package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Permittering
import no.nav.dagpenger.regel.Rettighetstype
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt.prøvingsdato

class PermitteringFastsettingSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett =
        listOf(
            Verneplikt.regelsett,
            Permittering.regelsett,
            PermitteringFastsetting.regelsett,
            Rettighetstype.regelsett,
            Søknadstidspunkt.regelsett,
        )
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker skal innvilges {boolsk} med permittering") { søkt: Boolean ->
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato))
            opplysninger.leggTil(Faktum(Permittering.oppfyllerKravetTilPermittering, søkt) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Så("skal søker få {int} uker med permittering") { uker: Int ->
            if (uker > 0) {
                opplysninger.finnOpplysning(PermitteringFastsetting.permitteringsperiode).verdi shouldBe uker
            }
        }
    }
}
