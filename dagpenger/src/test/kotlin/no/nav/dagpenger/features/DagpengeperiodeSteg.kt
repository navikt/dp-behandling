package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DagpengeperiodeSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett = listOf(Dagpengeperiode.regelsett, Minsteinntekt.regelsett)
    private val opplysninger: Opplysninger = Opplysninger()

    @BeforeStep
    fun kjørRegler() {
        Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at søker har har rett til dagpenger fra {string}") { dato: String ->
            val dato = LocalDate.parse(dato, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            opplysninger.leggTil(
                Faktum(
                    Søknadstidspunkt.søknadstidspunkt,
                    dato,
                ),
            )
        }
        Gitt("at søker har {string} siste 12 måneder") { inntekt: String ->
            opplysninger.leggTil(
                Faktum(
                    Minsteinntekt.inntekt12,
                    inntekt.toDouble(),
                ),
            )
        }

        Gitt("at søker har {string} siste 36 måneder") { inntekt: String ->
            opplysninger.leggTil(
                Faktum(
                    Minsteinntekt.inntekt36,
                    inntekt.toDouble(),
                ),
            )
        }

        Så("skal søker ha {int} uker med dagpenger") { uker: Int ->
            val faktum = opplysninger.finnOpplysning(Dagpengeperiode.antallStønadsuker)
            faktum.verdi shouldBe uker
        }
    }
}