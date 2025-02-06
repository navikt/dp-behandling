package no.nav.dagpenger.features

import io.cucumber.datatable.DataTable
import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.features.utils.somLocalDate
import no.nav.dagpenger.features.utils.tilBeløp
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.KravPåDagpenger.minsteinntektEllerVerneplikt
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.Verneplikt
import java.time.LocalDate

class MinsteinntektSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett = RegelverkDagpenger.regelsettFor(minsteinntektEllerVerneplikt)
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at søknadsdato er {string}") { søknadsdato: String ->
            opplysninger.leggTil(Faktum<LocalDate>(Søknadstidspunkt.søknadsdato, søknadsdato.somLocalDate()))
            opplysninger.leggTil(Faktum<LocalDate>(Søknadstidspunkt.ønsketdato, søknadsdato.somLocalDate()))
            regelkjøring.evaluer()
        }
        Gitt("at verneplikt er {boolsk}") { verneplikt: Boolean ->
            opplysninger.leggTil(Faktum<Boolean>(Verneplikt.avtjentVerneplikt, verneplikt)).also { regelkjøring.evaluer() }
        }
        Gitt("at inntekt er") { data: DataTable ->
            opplysninger.leggTil(Faktum<Beløp>(Minsteinntekt.inntekt12, data.cell(0, 1).tilBeløp()))
            opplysninger.leggTil(Faktum<Beløp>(opplysningstype = Minsteinntekt.inntekt36, verdi = data.cell(1, 1).tilBeløp()))
            regelkjøring.evaluer()
        }

        Så("skal utfallet til minste arbeidsinntekt være {boolsk}") { utfall: Boolean ->
            opplysninger.har(minsteinntektEllerVerneplikt) shouldBe true
            opplysninger.finnOpplysning(minsteinntektEllerVerneplikt).verdi shouldBe utfall
        }
    }

    private data class TestInntekt(
        val type: String,
        val beløp: Double,
    )
}
