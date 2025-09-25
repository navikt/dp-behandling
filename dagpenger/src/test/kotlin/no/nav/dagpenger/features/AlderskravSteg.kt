package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.features.utils.somLocalDate
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Alderskrav
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import org.junit.jupiter.api.Assertions
import java.time.LocalDate

class AlderskravSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett = listOf(Alderskrav.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at fødselsdatoen til søkeren er {string}") { fødselsdato: String ->
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Alderskrav.fødselsdato,
                        fødselsdato.somLocalDate(),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Gitt("at virkningstidspunktet er {string}") { virkningsdato: String ->
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Søknadstidspunkt.søknadsdato,
                        virkningsdato.somLocalDate(),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Søknadstidspunkt.ønsketdato,
                        virkningsdato.somLocalDate(),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Så("skal utfallet være {boolsk}") { utfall: Boolean ->
            Assertions.assertTrue(opplysninger.har(Alderskrav.kravTilAlder))
            Assertions.assertEquals(
                utfall,
                opplysninger.finnOpplysning(Alderskrav.kravTilAlder).verdi,
            )
        }
    }
}
