package no.nav.dagpenger.features

import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.Utdanning

class UtdanningSteg : No {
    private val fraDato = 22.mai(2024)
    private val regelsett = listOf(Utdanning.regelsett)
    private val opplysninger = Opplysninger()

    init {
        Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())

        Gitt("at personen søker på kravet om dagpenger") {
            opplysninger.leggTil(Faktum(Søknadstidspunkt.søknadsdato, 23.mai(2024)))
            opplysninger.leggTil(Faktum(Søknadstidspunkt.ønsketdato, 23.mai(2024)))
        }

        Gitt("at søkeren svarer {string} på spørsmålet om utdanning") { utdanning: String ->
            val verdi = oversett(utdanning)
            opplysninger.leggTil(Faktum(Utdanning.tarUtdanning, verdi))
        }

        Gitt(
            "søkeren har fått {string} unntak til kravet om utdanning",
        ) { unntak: String ->
            val verdi = oversett(unntak)
            opplysninger.leggTil(Faktum(Utdanning.godkjentUnntakForUtdanning, verdi))
        }
        Så("skal utfallet om utdanning være {string}") { string: String ->
            opplysninger.har(Utdanning.kravTilUtdanning) shouldBe true
            opplysninger.finnOpplysning(Utdanning.kravTilUtdanning).verdi shouldBe oversett(string)
        }
    }

    private fun oversett(utdanning: String) =
        when (utdanning) {
            "Ja" -> true
            "Nei" -> false
            else -> throw IllegalArgumentException("Ukjent svar på utdanning: $utdanning")
        }
}
