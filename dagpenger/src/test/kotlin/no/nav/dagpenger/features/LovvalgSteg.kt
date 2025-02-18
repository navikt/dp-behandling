package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Lovvalg
import no.nav.dagpenger.regel.Søknadstidspunkt

class LovvalgSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(Lovvalg.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("personen omfattet av trygdelovgivningen i Norge") {
            opplysninger.leggTil(Faktum(Lovvalg.erLovvalgNorge, true)).also { regelkjøring.evaluer() }
        }
        Gitt("personen omfattes ikke av trygdelovgivningen i Norge", {
            opplysninger.leggTil(Faktum(Lovvalg.erLovvalgNorge, false)).also { regelkjøring.evaluer() }
        })

        Gitt("saksbehandler har begrunnet med {string}", { begrunnelse: String ->
            opplysninger.leggTil(Faktum(Lovvalg.hvisIkkeNorge, begrunnelse)).also { regelkjøring.evaluer() }
        })

        Så("skal vilkåret om lovvalg være ikke oppfylt", {
            opplysninger.oppfyller(Lovvalg.erLovvalgNorge) shouldBe false
        })

        Så("skal vilkåret om lovvalg være oppfylt") {
            opplysninger.oppfyller(Lovvalg.erLovvalgNorge) shouldBe true
        }

        Så("begrunnelsen er {string}", { begrunnelse: String ->
            opplysninger.finnOpplysning(Lovvalg.hvisIkkeNorge).verdi shouldBe begrunnelse
        })
    }
}
