package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.Utdanning

class UtdanningSteg : No {
    private val fraDato = 23.mai(2024)
    private val regelsett = listOf(Utdanning.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()

    @BeforeStep
    fun kjørRegler() {
        Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at personen søker på kravet om dagpenger") {
            opplysninger.leggTil(Faktum(Søknadstidspunkt.søknadsdato, 23.mai(2024)))
            opplysninger.leggTil(Faktum(Søknadstidspunkt.ønsketdato, 23.mai(2024)))
        }

        Gitt("at søkeren svarer {boolsk} på spørsmålet om utdanning") { utdanning: Boolean ->
            opplysninger.leggTil(Faktum(Utdanning.tarUtdanning, utdanning))
        }

        Gitt("at unntaket arbeidsmarkedstiltak er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum(Utdanning.deltakelseIArbeidsmarkedstiltak, svar))
        }

        Gitt("at unntaket opplæring for innvandrere er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum(Utdanning.opplæringForInnvandrere, svar))
        }

        Gitt("at unntaket grunnskoleopplæring er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum(Utdanning.grunnskoleopplæring, svar))
        }

        Gitt("at unntaket høyere yrkesfaglig utdanning er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum(Utdanning.høyereYrkesfagligUtdanning, svar))
        }

        Gitt("at unntaket høyere utdanning er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum(Utdanning.høyereUtdanning, svar))
        }

        Gitt("at unntaket deltar på kurs er {boolsk}") { svar: Boolean ->
            opplysninger.leggTil(Faktum(Utdanning.deltakelsePåKurs, svar))
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
