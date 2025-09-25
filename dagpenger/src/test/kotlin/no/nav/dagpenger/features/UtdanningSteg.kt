package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Utdanning
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import java.time.LocalDate

class UtdanningSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(Utdanning.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at personen søker på kravet om dagpenger") {
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(Søknadstidspunkt.søknadsdato, 23.mai(2024)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(Søknadstidspunkt.ønsketdato, 23.mai(2024)) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Gitt("at søkeren svarer {boolsk} på spørsmålet om utdanning") { utdanning: Boolean ->
            opplysninger.leggTil(Faktum<Boolean>(Utdanning.tarUtdanning, utdanning) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Gitt("at unntaket arbeidsmarkedstiltak er {boolsk}") { svar: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Utdanning.deltakelseIArbeidsmarkedstiltak, svar) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Gitt("at unntaket opplæring for innvandrere er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum<Boolean>(Utdanning.opplæringForInnvandrere, svar) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Gitt("at unntaket grunnskoleopplæring er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum<Boolean>(Utdanning.grunnskoleopplæring, svar) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Gitt("at unntaket høyere yrkesfaglig utdanning er {boolsk}") { svar: Boolean ->
            opplysninger
                .leggTil(
                    Faktum<Boolean>(Utdanning.høyereYrkesfagligUtdanning, svar) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Gitt("at unntaket høyere utdanning er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum<Boolean>(Utdanning.høyereUtdanning, svar) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Gitt("at unntaket deltar på kurs er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum<Boolean>(Utdanning.deltakelsePåKurs, svar) as Opplysning<*>).also { regelkjøring.evaluer() }
        }

        Så("skal utfallet om utdanning være {boolsk}") { svar: Boolean ->
            withClue("${Utdanning.kravTilUtdanning} skal være tilstede") {
                opplysninger.har(Utdanning.kravTilUtdanning) shouldBe true
            }
            withClue("${Utdanning.kravTilUtdanning} skal være $svar") {
                opplysninger.finnOpplysning(Utdanning.kravTilUtdanning).verdi shouldBe svar
            }
        }
    }
}
