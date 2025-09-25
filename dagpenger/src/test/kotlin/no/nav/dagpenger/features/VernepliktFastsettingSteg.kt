package no.nav.dagpenger.features

import io.cucumber.datatable.DataTable
import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.features.utils.tilBeløp
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Minsteinntekt.inntekt12
import no.nav.dagpenger.regel.Minsteinntekt.inntekt36
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Rettighetstype.skalVernepliktVurderes
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnlag
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnlag12mnd
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag.grunnlag36mnd
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode.ordinærPeriode
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.grunnlagForVernepliktErGunstigst
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting.vernepliktPeriode

class VernepliktFastsettingSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett =
        RegelverkDagpenger.regelsettFor(grunnlag) +
            RegelverkDagpenger.regelsettFor(ordinærPeriode) +
            RegelverkDagpenger.regelsettFor(grunnlagForVernepliktErGunstigst) +
            RegelverkDagpenger.regelsettFor(vernepliktPeriode)
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at kravet til verneplikt er {boolsk}") { verneplikt: Boolean ->
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato))
            opplysninger.leggTil(Faktum(oppfyllerKravetTilVerneplikt, verneplikt))
            opplysninger.leggTil(Faktum(skalVernepliktVurderes, verneplikt))
            regelkjøring.evaluer()
        }

        Gitt("at inntekt ved verneplikt er") { data: DataTable ->
            opplysninger.leggTil(Faktum<Beløp>(inntekt12, data.cell(0, 1).tilBeløp()))
            opplysninger.leggTil(Faktum<Beløp>(inntekt36, data.cell(1, 1).tilBeløp()))
            opplysninger.leggTil(Faktum<Beløp>(grunnlag12mnd, data.cell(0, 1).tilBeløp()))
            opplysninger.leggTil(Faktum<Beløp>(grunnlag36mnd, data.cell(1, 1).tilBeløp()))
            regelkjøring.evaluer()
        }

        Så("skal grunnlag være {string}") { grunnlag: String ->
            opplysninger.finnOpplysning(Dagpengegrunnlag.grunnlag).verdi shouldBe Beløp(grunnlag.toInt())
        }

        Så("dagpengerperioden være {int} uker") { antallUker: Int ->
            opplysninger.finnOpplysning(ordinærPeriode).verdi shouldBe antallUker
        }

        Så("vernepliktperioden være {int} uker") { antallUker: Int ->
            if (opplysninger.finnOpplysning(grunnlagForVernepliktErGunstigst).verdi == true) {
                opplysninger.finnOpplysning(vernepliktPeriode).verdi shouldBe antallUker
            }
        }
    }
}
