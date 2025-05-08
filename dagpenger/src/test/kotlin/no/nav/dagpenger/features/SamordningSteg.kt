package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.features.utils.opplysningerTilRegelkjøring
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Barn
import no.nav.dagpenger.opplysning.verdier.BarnListe
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Samordning
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.ukessats
import java.time.LocalDate

class SamordningSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett =
        RegelverkDagpenger.regelsettFor(
            ukessats,
        )
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring =
            Regelkjøring(fraDato, opplysninger, opplysningerTilRegelkjøring, *regelsett.toTypedArray())
    }

    init {

        Gitt("at søker har søkt om dagpenger og har redusert ytelse") {
            opplysninger
                .leggTil(
                    Faktum(Søknadstidspunkt.søknadsdato, 11.mai(2022)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum(Søknadstidspunkt.ønsketdato, 11.mai(2022)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Gitt("har {int} barn") { antall: Int ->
            val barn =
                (1..antall).map {
                    Barn(
                        fødselsdato = LocalDate.now(),
                        fornavnOgMellomnavn = "Donlald",
                        etternavn = "Duck $it",
                        statsborgerskap = "NOR",
                        kvalifiserer = true,
                    )
                }

            opplysninger.leggTil(Faktum(DagpengenesStørrelse.barn, BarnListe(barn))).also { regelkjøring.evaluer() }
        }

        Gitt("søker har redusert sykepenger {boolsk}") { sykepenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(Samordning.sykepenger, sykepenger),
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
                    Faktum(Samordning.pleiepenger, pleiepenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert omsorgspenger {boolsk}") { omsorgspenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(Samordning.omsorgspenger, omsorgspenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert opplæringspenger {boolsk}") { opplæringspenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(Samordning.opplæringspenger, opplæringspenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert uføre {boolsk}") { uføre: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(Samordning.uføre, uføre),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert foreldrepenger {boolsk}") { foreldrepenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(Samordning.foreldrepenger, foreldrepenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }

        Gitt("søker har redusert svangerskapspenger {boolsk}") { svangerskapspenger: Boolean ->
            opplysninger
                .leggTil(
                    Faktum(Samordning.svangerskapspenger, svangerskapspenger),
                ).also {
                    regelkjøring.evaluer()
                }
        }
        Så("skal søker få samordnet dagsats {boolsk}") { samordnet: Boolean ->
            opplysninger.finnOpplysning(Samordning.skalSamordnes).verdi shouldBe samordnet
        }

        Så("gitt at bruker har {string} i grunnlag") { beløp: String ->
            opplysninger
                .leggTil(
                    Faktum(Dagpengegrunnlag.grunnlag, Beløp(beløp.toBigDecimal())),
                ).also { regelkjøring.evaluer() }
        }

        Så("skal at bruker ha {string} i samordnet dagsats") { beløp: String ->
            opplysninger.finnOpplysning(Samordning.samordnetDagsats).verdi shouldBe Beløp(beløp.toBigDecimal())
        }

        Så("utfall etter samordning skal være {boolsk}") { utfall: Boolean ->
            opplysninger.finnOpplysning(Samordning.utfallEtterSamordning).verdi shouldBe utfall
        }
    }
}
