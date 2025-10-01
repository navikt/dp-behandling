package no.nav.dagpenger.features

import io.cucumber.java8.No
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.features.utils.somLocalDate
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.RegistrertArbeidssøker
import no.nav.dagpenger.regel.RegistrertArbeidssøker.registrertArbeidssøker
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.Virkningstidspunkt
import org.junit.jupiter.api.Assertions.assertTrue

class RegistrertArbeidssøkerSteg : No {
    private val regelsett = listOf(RegistrertArbeidssøker.regelsett, Søknadstidspunkt.regelsett, Virkningstidspunkt.regelsett)
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    init {
        Gitt("at personen søkte om dagpenger den {string}") { søknadsdato: String ->
            regelkjøring = Regelkjøring(søknadsdato.somLocalDate(), opplysninger, *regelsett.toTypedArray())
            opplysninger
                .leggTil(Faktum(Søknadstidspunkt.søknadsdato, søknadsdato.somLocalDate()) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(Faktum(Søknadstidspunkt.ønsketdato, søknadsdato.somLocalDate()) as Opplysning<*>)
                .also { regelkjøring.evaluer() }
        }
        Gitt("personen var registrert? {boolsk} på {string}") { svar: Boolean, registrert: String ->
            opplysninger
                .leggTil(
                    Faktum(
                        registrertArbeidssøker,
                        svar,
                        Gyldighetsperiode(fom = registrert.somLocalDate()),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Så("er kravet til å være registrert arbeidssøker {string}") { utfall: String ->

            withClue("Forventer at vi har '${registrertArbeidssøker.navn}'") {
                opplysninger.har(registrertArbeidssøker) shouldBe true
            }
            assertTrue(opplysninger.har(registrertArbeidssøker))

            val verdi = opplysninger.finnOpplysning(registrertArbeidssøker).verdi

            withClue("Forventet at kravet til meldeplikt skulle være $utfall") {
                when (Utfall.valueOf(utfall)) {
                    Utfall.Oppfylt -> verdi shouldBe true
                    Utfall.`Ikke oppfylt` -> verdi shouldBe false
                }
            }
        }
    }

    private enum class Utfall {
        Oppfylt,

        @Suppress("ktlint:standard:enum-entry-name-case")
        `Ikke oppfylt`,
    }
}
