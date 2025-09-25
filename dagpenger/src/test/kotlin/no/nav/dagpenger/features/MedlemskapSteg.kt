package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt.prøvingsdato

class MedlemskapSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(Opphold.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker har søkt om dagpenger og er medlem?") {
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato)).also { regelkjøring.evaluer() }
        }

        Og("at personen er medlem {boolsk} i folketrygden") { medlem: Boolean ->
            opplysninger.leggTil(Faktum(Opphold.medlemFolketrygden, medlem) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Så("skal vilkåret om medlemskap være {boolsk}") { utfall: Boolean ->
            val faktum = opplysninger.finnOpplysning(Opphold.oppfyllerMedlemskap)
            withClue("Vilkåret om medlemskap skal være $utfall") {
                faktum.verdi shouldBe utfall
            }
        }
    }
}
