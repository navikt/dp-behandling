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

class PermitteringSteg : No {
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
        Gitt("at søker har {boolsk} om dagpenger under permittering") { søkt: Boolean ->
            opplysninger.leggTil(Faktum(Rettighetstype.erPermittert, søkt) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Og("saksbehandler vurderer at søker har {boolsk} til permittering") { årsak: Boolean ->
            opplysninger.leggTil(Faktum(Permittering.godkjentPermitteringsårsak, årsak) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Og("vurderer at søker har {boolsk} permittering") { midlertidig: Boolean ->
            opplysninger
                .leggTil(Faktum(Permittering.erPermitteringenMidlertidig, midlertidig) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }

        Så("skal søker få {boolsk} av permittering") { utfall: Boolean ->
            opplysninger.finnOpplysning(Permittering.oppfyllerKravetTilPermittering).verdi shouldBe utfall
        }
    }
}
