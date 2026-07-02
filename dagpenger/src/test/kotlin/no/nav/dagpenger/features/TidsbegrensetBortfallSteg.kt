package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall

class TidsbegrensetBortfallSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = RegelverkDagpenger.regelsettFor(TidsbegrensetBortfall.antallBortfallsdager)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker har søkt om dagpenger for vurdering av tidsbegrenset bortfall") {
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato)).also { regelkjøring.evaluer() }
        }
        Og("saksbehandler ilegger tidsbegrenset bortfall") {
            opplysninger.leggTil(Faktum(TidsbegrensetBortfall.harTidsbegrensetBortfall, true)).also { regelkjøring.evaluer() }
        }
        Og("saksbehandler ilegger ikke tidsbegrenset bortfall") {
            opplysninger.leggTil(Faktum(TidsbegrensetBortfall.harTidsbegrensetBortfall, false)).also { regelkjøring.evaluer() }
        }
        Og("saksbehandler ilegger tidsbegrenset bortfall i {string} uker") { uker: String ->
            opplysninger.leggTil(Faktum(TidsbegrensetBortfall.harTidsbegrensetBortfall, true))
            opplysninger.leggTil(Faktum(TidsbegrensetBortfall.antallBortfallsuker, uker.toInt())).also { regelkjøring.evaluer() }
        }
        Så("skal antall bortfallsdager være {string}") { dager: String ->
            opplysninger.finnOpplysning(TidsbegrensetBortfall.antallBortfallsdager).verdi shouldBe dager.toInt()
        }
    }
}
