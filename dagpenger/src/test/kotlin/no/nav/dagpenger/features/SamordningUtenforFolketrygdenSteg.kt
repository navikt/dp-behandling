package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.juni
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.arbeidsdagerPerUke
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.andreYtelser
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.andreØkonomiskeYtelser
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.nedreGrenseForSamordning
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.samordnetUkessats
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.samordnetUkessatsUtenBarnetillegg
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.skalSamordnesUtenforFolketrygden
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.sumAvYtelserUtenforFolketrygden

class SamordningUtenforFolketrygdenSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett =
        RegelverkDagpenger.regelsettFor(
            skalSamordnesUtenforFolketrygden,
        )
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {
        Gitt("at søker har søkt om dagpenger med andre ytelser") {
            opplysninger
                .leggTil(
                    Faktum(Søknadstidspunkt.søknadsdato, 11.juni(2024)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum(Søknadstidspunkt.ønsketdato, 11.juni(2024)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Gitt("søker har oppgitt ytelse {string}") { ytelse: String ->
            opplysninger.leggTil(Faktum(andreØkonomiskeYtelser, false))
            when (ytelse) {
                else -> opplysninger.leggTil(Faktum(andreYtelser, true)).also { regelkjøring.evaluer() }
            }
        }

        Gitt("søker har ukessats {string}") { sats: String ->
            opplysninger.leggTil(Faktum(samordnetUkessatsUtenBarnetillegg, Beløp(sats.toBigDecimal())))
        }

        Gitt("søker har oppgitt ytelse med {string} utbetalt per uke") { beløp: String ->
            opplysninger.leggTil(Faktum(andreYtelser, true))
            opplysninger.leggTil(Faktum(sumAvYtelserUtenforFolketrygden, Beløp(beløp.toBigDecimal())))
            regelkjøring.evaluer()
        }

        Så("skal vi endre ukessats til {string}") { nySats: String ->
            opplysninger.finnOpplysning(nedreGrenseForSamordning).verdi shouldBe Beløp(3720.84)
            opplysninger.finnOpplysning(samordnetUkessats).verdi shouldBe Beløp(nySats.toBigDecimal())
        }
        Så("skal vi endre dagsats til {string}") { nySats: String ->
            opplysninger.finnOpplysning(samordnetUkessats).verdi / opplysninger.finnOpplysning(arbeidsdagerPerUke).verdi shouldBe
                Beløp(nySats.toBigDecimal())
        }

        Så("skal vi kreve samordning") {
            opplysninger.finnOpplysning(skalSamordnesUtenforFolketrygden).verdi shouldBe true
        }
    }
}
