package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Virkningstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.antallBarn
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsUtenBarnetillegg
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.ukessats

class DagpengensStørrelseSteg : No {
    private val fraDato = 10.mai(2024)
    private val regelsett = RegelverkDagpenger.regelsettFor(ukessats)
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at dagpengegrunnlag er {string}") { grunnlag: String ->
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato)).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum(
                        Dagpengegrunnlag.grunnlag,
                        Beløp(grunnlag.toBigDecimal()),
                    ),
                ).also { regelkjøring.evaluer() }
        }

        Og("at søker har ikke barn") {
            opplysninger.leggTil(Faktum(antallBarn, 0)).also { regelkjøring.evaluer() }
        }

        Gitt("at søker har {int} barn") { antall: Int ->
            opplysninger.leggTil(Faktum(antallBarn, antall)).also { regelkjøring.evaluer() }
        }

        Så("skal dagpengens uavrundet størrelse uten barnetillegg være {string}") { størrelse: String ->
            opplysninger.finnOpplysning(dagsatsUtenBarnetillegg).verdi shouldBe Beløp(størrelse.toBigDecimal())
        }

        Så("skal dagpengens størrelse være {string}") { størrelse: String ->
            opplysninger.finnOpplysning(dagsatsEtterSamordningMedBarnetillegg).verdi shouldBe Beløp(størrelse.toBigDecimal())
        }

        Så("skal ukessats være {string}") { ukessats: String ->
            opplysninger.finnOpplysning(DagpengenesStørrelse.ukessats).verdi shouldBe Beløp(ukessats.toBigDecimal())
        }
    }
}
