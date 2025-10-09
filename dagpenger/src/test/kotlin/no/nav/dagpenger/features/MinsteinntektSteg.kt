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
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.boolsk
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.dsl.vilkår
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.tomHjemmel
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.OpplysningsTyper.MinsteinntektEllerVernepliktId
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt

private val minsteinntektEllerVerneplikt = boolsk(MinsteinntektEllerVernepliktId, "Oppfyller kravet til minsteinntekt eller verneplikt")

class MinsteinntektSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett =
        RegelverkDagpenger.regelsettFor(minsteinntekt) + RegelverkDagpenger.regelsettFor(oppfyllerKravetTilVerneplikt) +
            vilkår(tomHjemmel("foo")) {
                utfall(minsteinntektEllerVerneplikt) { enAv(minsteinntekt, oppfyllerKravetTilVerneplikt) }
            }
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at søknadsdato er {string}") { søknadsdato: String ->
            opplysninger.leggTil(Faktum(Søknadstidspunkt.søknadsdato, søknadsdato.somLocalDate()))
            opplysninger.leggTil(Faktum(Søknadstidspunkt.ønsketdato, søknadsdato.somLocalDate()))
            regelkjøring.evaluer()
        }
        Gitt("at verneplikt er {boolsk}") { verneplikt: Boolean ->
            opplysninger.leggTil(Faktum(Verneplikt.avtjentVerneplikt, verneplikt)).also { regelkjøring.evaluer() }
        }
        Gitt("at inntekt er") { data: DataTable ->
            opplysninger.leggTil(Faktum(Minsteinntekt.inntekt12, data.cell(0, 1).tilBeløp()))
            opplysninger.leggTil(Faktum(opplysningstype = Minsteinntekt.inntekt36, verdi = data.cell(1, 1).tilBeløp()))
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
