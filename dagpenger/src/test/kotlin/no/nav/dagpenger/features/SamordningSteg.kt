package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Samordning
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.avrundetDagsMedBarnetillegg
import java.time.LocalDate

class SamordningSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett =
        listOf(Samordning.regelsett) +
            RegelverkDagpenger.regelsettFor(
                avrundetDagsMedBarnetillegg,
            )
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at søker har søkt om dagpenger og har redusert ytelse") {
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(Søknadstidspunkt.søknadsdato, 11.mai(2022)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(Søknadstidspunkt.ønsketdato, 11.mai(2022)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Gitt("søker har redusert sykepenger {boolsk}") { sykepenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Samordning.sykepenger, sykepenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("dagsats for sykepenger er {string}") { beløp: String ->
            opplysninger
                .leggTil(
                    Faktum(Samordning.sykepengerDagsats, Beløp(beløp.toBigDecimal())),
                ).also {
                    regelkjøring.evaluer()
                }
        }
        Gitt("søker har redusert pleiepenger {boolsk}") { pleiepenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Samordning.pleiepenger, pleiepenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert omsorgspenger {boolsk}") { omsorgspenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Samordning.omsorgspenger, omsorgspenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert opplæringspenger {boolsk}") { opplæringspenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Samordning.opplæringspenger, opplæringspenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert uføre {boolsk}") { uføre: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Samordning.uføre, uføre),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert foreldrepenger {boolsk}") { foreldrepenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Samordning.foreldrepenger, foreldrepenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert svangerskapspenger {boolsk}") { svangerskapspenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Samordning.svangerskapspenger, svangerskapspenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }
        Så("skal søker få samordnet dagsats {boolsk}") { samordnet: Boolean ->
            opplysninger.finnOpplysning(Samordning.skalSamordnes).verdi shouldBe samordnet
        }

        Så("gitt at bruker har {string} i dagsats") { beløp: String ->
            opplysninger
                .leggTil(
                    Faktum(DagpengenesStørrelse.avrundetDagsUtenBarnetillegg, Beløp(beløp.toBigDecimal())),
                ).also { regelkjøring.evaluer() }
        }

        Så("skal at bruker ha {string} i samordnet dagsats") { beløp: String ->
            opplysninger.finnOpplysning(Samordning.samordnetDagsats).verdi shouldBe Beløp(beløp.toBigDecimal())
        }
    }
}