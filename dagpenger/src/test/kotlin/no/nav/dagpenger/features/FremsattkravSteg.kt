package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.september
import no.nav.dagpenger.features.utils.somLocalDate
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.HarFramsattKrav
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import java.time.LocalDate

class FremsattkravSteg : No {
    private val fraDato = 26.september(2025)

    private val regelsett = listOf(Søknadstidspunkt.regelsett, HarFramsattKrav.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt(
            "søknadstidspunktet er {string}",
        ) { søknadstidspunkt: String ->
            val verdi = søknadstidspunkt.somLocalDate()
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Søknadstidspunkt.søknadsdato,
                        verdi,
                        Gyldighetsperiode(fraOgMed = verdi),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Søknadstidspunkt.ønsketdato,
                        verdi,
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Så(
            "forelegger det plikt til å vurdere rett til ny stønadsperiode",
        ) {
            opplysninger.har(HarFramsattKrav.harFramsattKrav) shouldBe true
            val harFramsattKrav = opplysninger.finnOpplysning(HarFramsattKrav.harFramsattKrav)
            harFramsattKrav.verdi shouldBe true
            val fraDato = opplysninger.finnOpplysning(Søknadstidspunkt.søknadstidspunkt).verdi
            harFramsattKrav.gyldighetsperiode.fraOgMed shouldBe fraDato
        }

        Gitt("trekker kravet om dagpenger") {
            opplysninger
                .leggTil(
                    Faktum(
                        HarFramsattKrav.harFramsattKrav,
                        false,
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Så(
            "bortfaller plikten til å vurdere rett til ny stønadsperiode",
        ) {
            opplysninger.har(HarFramsattKrav.harFramsattKrav) shouldBe true
            val harFramsattKrav = opplysninger.finnOpplysning(HarFramsattKrav.harFramsattKrav)
            harFramsattKrav.verdi shouldBe false
        }
    }
}
