package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.ReellArbeidssøker
import no.nav.dagpenger.regel.Søknadstidspunkt
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate

class ReellArbeidssøkerSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett = listOf(ReellArbeidssøker.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at personen søker dagpenger") {
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(Søknadstidspunkt.søknadsdato, 11.mai(2022)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(Søknadstidspunkt.ønsketdato, 11.mai(2022)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Gitt("kan jobbe både heltid og deltid") {
            opplysninger.leggTil(Faktum<Boolean>(ReellArbeidssøker.kanJobbeDeltid, true) as Opplysning<*>).also { regelkjøring.evaluer() }
        }
        Gitt("kan ikke jobbe både heltid og deltid") {
            opplysninger.leggTil(Faktum<Boolean>(ReellArbeidssøker.kanJobbeDeltid, false) as Opplysning<*>).also { regelkjøring.evaluer() }
        }
        Gitt("kan jobbe i hele Norge") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.kanJobbeHvorSomHelst, true) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Gitt("kan ikke jobbe i hele Norge") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.kanJobbeHvorSomHelst, false) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Gitt("kan ta alle typer arbeid") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.erArbeidsfør, true) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Gitt("kan ikke ta alle typer arbeid") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.erArbeidsfør, false) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Men("oppfyller kravet å kun søke lokalt arbeid") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.godkjentLokalArbeidssøker, true) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Men("oppfyller kravet til å kun søke deltidssarbeid") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.godkjentDeltidssøker, true) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Gitt("er villig til å bytte yrke eller gå ned i lønn") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.villigTilEthvertArbeid, true) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Gitt("er ikke villig til å bytte yrke eller gå ned i lønn") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.villigTilEthvertArbeid, false) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Så("skal kravet til reell arbeidssøker være oppfylt") {
            assertTrue(opplysninger.har(ReellArbeidssøker.kravTilArbeidssøker))
            val verdi = opplysninger.finnOpplysning(ReellArbeidssøker.kravTilArbeidssøker).verdi
            assertTrue(verdi)
        }
        Så("skal kravet til reell arbeidssøker ikke være oppfylt") {
            assertTrue(opplysninger.har(ReellArbeidssøker.kravTilArbeidssøker))
            val verdi = opplysninger.finnOpplysning(ReellArbeidssøker.kravTilArbeidssøker).verdi
            assertFalse(verdi)
        }
    }
}
