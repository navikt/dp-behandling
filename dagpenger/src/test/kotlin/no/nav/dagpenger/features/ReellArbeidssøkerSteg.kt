package no.nav.dagpenger.features

import io.cucumber.java8.No
import no.nav.dagpenger.features.utils.somLocalDate
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.ReellArbeidssøker
import no.nav.dagpenger.regel.Søknadstidspunkt
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue

class ReellArbeidssøkerSteg : No {
    private val regelsett = listOf(ReellArbeidssøker.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    init {
        Gitt("at personen søkte {string}") { søknadsdato: String ->
            regelkjøring = Regelkjøring(søknadsdato.somLocalDate(), opplysninger, *regelsett.toTypedArray())
            opplysninger.leggTil(Faktum(Søknadstidspunkt.søknadsdato, søknadsdato.somLocalDate()))
            opplysninger.leggTil(Faktum(Søknadstidspunkt.ønsketdato, søknadsdato.somLocalDate()))
            regelkjøring.evaluer()
        }
        Gitt("ønsker arbeidstid på {float} timer") { timer: Float ->
            opplysninger.leggTil(Faktum(ReellArbeidssøker.ønsketArbeidstid, timer.toDouble()))
        }
        Gitt("kan jobbe både heltid og deltid") {
            opplysninger.leggTil(Faktum(ReellArbeidssøker.kanJobbeDeltid, true)).also { regelkjøring.evaluer() }
        }
        Gitt("kan ikke jobbe både heltid og deltid") {
            opplysninger.leggTil(Faktum(ReellArbeidssøker.kanJobbeDeltid, false)).also { regelkjøring.evaluer() }
        }
        Gitt("kan jobbe i hele Norge") {
            opplysninger.leggTil(Faktum(ReellArbeidssøker.kanJobbeHvorSomHelst, true)).also { regelkjøring.evaluer() }
        }
        Gitt("kan ikke jobbe i hele Norge") {
            opplysninger.leggTil(Faktum(ReellArbeidssøker.kanJobbeHvorSomHelst, false)).also { regelkjøring.evaluer() }
        }
        Gitt("kan ta alle typer arbeid") {
            opplysninger.leggTil(Faktum(ReellArbeidssøker.erArbeidsfør, true)).also { regelkjøring.evaluer() }
        }
        Gitt("kan ikke ta alle typer arbeid") {
            opplysninger.leggTil(Faktum(ReellArbeidssøker.erArbeidsfør, false)).also { regelkjøring.evaluer() }
        }
        Men("oppfyller kravet å kun søke lokalt arbeid") {
            opplysninger
                .leggTil(Faktum(ReellArbeidssøker.godkjentLokalArbeidssøker, true))
                .also { regelkjøring.evaluer() }
        }
        Men("oppfyller kravet til å kun søke deltidssarbeid") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.godkjentDeltidssøker, true),
                ).also { regelkjøring.evaluer() }
        }
        Gitt("er villig til å bytte yrke eller gå ned i lønn") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.villigTilEthvertArbeid, true),
                ).also { regelkjøring.evaluer() }
        }
        Gitt("er ikke villig til å bytte yrke eller gå ned i lønn") {
            opplysninger
                .leggTil(
                    Faktum<Boolean>(ReellArbeidssøker.villigTilEthvertArbeid, false),
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

    private enum class Utfall {
        Oppfylt,

        @Suppress("ktlint:standard:enum-entry-name-case")
        `Ikke oppfylt`,
    }
}
