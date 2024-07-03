package no.nav.dagpenger.features

import io.cucumber.datatable.DataTable
import io.cucumber.java8.No
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.features.utils.somLocalDate
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadsdato
import no.nav.dagpenger.regel.Verneplikt
import org.junit.jupiter.api.Assertions

class MinsteinntektSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett = listOf(Minsteinntekt.regelsett, Søknadstidspunkt.regelsett, Verneplikt.regelsett)
    private val opplysninger: Opplysninger = Opplysninger()

    init {
        Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())

        Gitt("at søknadsdato er {string}") { søknadsdato: String ->
            opplysninger.leggTil(
                Faktum(
                    Søknadstidspunkt.søknadsdato,
                    søknadsdato.somLocalDate(),
                ),
            )
            opplysninger.leggTil(
                Faktum(
                    Søknadstidspunkt.ønsketdato,
                    søknadsdato.somLocalDate(),
                ),
            )
        }
        Gitt("at verneplikt er {string}") { verneplikt: String ->
            val verdi =
                when (verneplikt) {
                    "Ja" -> true
                    "Nei" -> false
                    else -> throw IllegalArgumentException("Ukjent svar på verneplikt: $verneplikt")
                }
            opplysninger.leggTil(Faktum(Verneplikt.avtjentVerneplikt, verdi))
        }
        Gitt("at inntekt er") { data: DataTable ->

            opplysninger.leggTil(
                Faktum(
                    opplysningstype = Minsteinntekt.inntekt12,
                    verdi = data.cell(0, 1).toDouble(),
                ),
            )
            opplysninger.leggTil(
                Faktum(
                    opplysningstype = Minsteinntekt.inntekt36,
                    verdi = data.cell(1, 1).toDouble(),
                ),
            )
        }

        Så("skal utfallet til minste arbeidsinntekt være {string}") { utfall: String ->
            val verdi =
                when (utfall) {
                    "Ja" -> true
                    "Nei" -> false
                    else -> throw IllegalArgumentException("Ukjent utfall: $utfall")
                }
            Assertions.assertTrue(opplysninger.har(Minsteinntekt.minsteinntekt))
            Assertions.assertEquals(
                verdi,
                opplysninger.finnOpplysning(Minsteinntekt.minsteinntekt).verdi,
            )
        }
    }

    private data class TestInntekt(
        val type: String,
        val beløp: Double,
    )
}
