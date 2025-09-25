package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.features.utils.somLocalDate
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Opphold
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDate

class OppholdINorgeSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett = listOf(Opphold.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at søknadsdatos er {string}") { søknadsdato: String ->
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Søknadstidspunkt.søknadsdato,
                        søknadsdato.somLocalDate(),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Søknadstidspunkt.ønsketdato,
                        søknadsdato.somLocalDate(),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Gitt("at personen oppholder seg i Norge") {
            opplysninger.leggTil(Faktum<Boolean>(Opphold.oppholdINorge, true) as Opplysning<*>).also { regelkjøring.evaluer() }
        }
        Så("skal vilkåret om opphold i Norge være oppfylt") {
            assertTrue(opplysninger.har(Opphold.oppfyllerKravet))
            assertTrue(opplysninger.finnOpplysning(Opphold.oppfyllerKravet).verdi)
        }
        Gitt("at personen oppholder seg ikke i Norge") {
            opplysninger.leggTil(Faktum<Boolean>(Opphold.oppholdINorge, false) as Opplysning<*>).also { regelkjøring.evaluer() }
        }
        Så("skal vilkåret om opphold i Norge ikke være oppfylt") {
            assertTrue(opplysninger.har(Opphold.oppfyllerKravet))
            assertFalse(opplysninger.finnOpplysning(Opphold.oppfyllerKravet).verdi)
        }
        Men("at personen oppfyller ett unntak for opphold") {
            opplysninger.leggTil(Faktum<Boolean>(Opphold.unntakForOpphold, true) as Opplysning<*>).also { regelkjøring.evaluer() }
        }
    }
}
