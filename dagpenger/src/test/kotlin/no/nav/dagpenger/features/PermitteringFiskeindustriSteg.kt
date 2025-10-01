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
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.Virkningstidspunkt

class PermitteringFiskeindustriSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett =
        listOf(
            PermitteringFraFiskeindustrien.regelsett,
            Rettighetstype.regelsett,
            Søknadstidspunkt.regelsett,
            Virkningstidspunkt.regelsett,
            Verneplikt.regelsett,
        )
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker har {boolsk} om dagpenger under permittering fra fiskeindustrien") { søkt: Boolean ->
            opplysninger.leggTil(Faktum(Rettighetstype.permitteringFiskeforedling, søkt) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Og("saksbehandler vurderer at søker har {boolsk} til permittering fra fiskeindustrien") { årsak: Boolean ->
            opplysninger
                .leggTil(Faktum(PermitteringFraFiskeindustrien.godkjentÅrsakPermitteringFraFiskindustri, årsak) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Og("vurderer at søker har {boolsk} permittering fra fiskeindustrien") { midlertidig: Boolean ->
            opplysninger
                .leggTil(Faktum(PermitteringFraFiskeindustrien.erPermitteringenFraFiskeindustriMidlertidig, midlertidig) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Så("skal søker få {boolsk} av permittering fra fiskeindustrien") { utfall: Boolean ->
            opplysninger.finnOpplysning(PermitteringFraFiskeindustrien.oppfyllerKravetTilPermitteringFiskeindustri).verdi shouldBe utfall
        }
    }
}
