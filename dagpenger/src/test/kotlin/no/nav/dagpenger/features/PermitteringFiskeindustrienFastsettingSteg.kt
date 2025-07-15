package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien
import no.nav.dagpenger.regel.Rettighetstype
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.fastsetting.PermitteringFraFiskeindustrienFastsetting

class PermitteringFiskeindustrienFastsettingSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett =
        listOf(
            Rettighetstype.regelsett,
            PermitteringFraFiskeindustrien.regelsett,
            PermitteringFraFiskeindustrienFastsetting.regelsett,
            Søknadstidspunkt.regelsett,
            Verneplikt.regelsett,
        )
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker skal innvilges {boolsk} med permittering fra fiskeindustrien") { søkt: Boolean ->
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato))
            opplysninger
                .leggTil(Faktum(PermitteringFraFiskeindustrien.oppfyllerKravetTilPermitteringFiskeindustri, søkt) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Så("skal søker få {int} uker med permittering fra fiskeindustrien") { uker: Int ->
            if (uker > 0) {
                opplysninger.finnOpplysning(PermitteringFraFiskeindustrienFastsetting.permitteringFraFiskeindustriPeriode).verdi shouldBe
                    uker
            }
        }
    }
}
