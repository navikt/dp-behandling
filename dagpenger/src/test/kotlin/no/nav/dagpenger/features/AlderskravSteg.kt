package no.nav.dagpenger.features

import io.cucumber.java8.No
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Alderskrav
import no.nav.dagpenger.regel.Søknadstidspunkt
import org.junit.jupiter.api.Assertions
import java.time.LocalDate

class AlderskravSteg : No {
    private lateinit var fraDato: LocalDate
    private val regelsett = listOf(Alderskrav.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()
    private val regelkjøring: Regelkjøring get() = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())

    init {

        Gitt("at fødselsdatoen til søkeren er {dato}") { fødselsdato: LocalDate ->
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Alderskrav.fødselsdato,
                        fødselsdato,
                    ),
                )
        }
        Gitt("at virkningstidspunktet er {dato}") { virkningsdato: LocalDate ->
            fraDato = virkningsdato
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Søknadstidspunkt.søknadsdato,
                        virkningsdato,
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        Søknadstidspunkt.ønsketdato,
                        virkningsdato,
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
