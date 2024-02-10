package no.nav.dagpenger.opplysning.nrh

import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.mai
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PraktiskLydighetSteps : No {
    private val fraDato = 10.mai.atStartOfDay()
    private val opplysninger = Opplysninger()
    val regelkjøring = Regelkjøring(fraDato, opplysninger, BliPåPlassVilkår.regelsett)

    init {

        Gitt("at hunden ikke forflytte seg mer enn én hundelengde før øvelsen er slutt er {string}") { resultat: String ->
            opplysninger.leggTil(Faktum(BliPåPlassVilkår.hundenFlytterSegIkke, resultat.toBoolean()))
        }

        Gitt("at minutter hundefører skal være ut av synet er {string}") { minutter: String ->
            opplysninger.leggTil(Faktum(BliPåPlassVilkår.minutterHundeførerSkalVæreUtAvSynet, minutter.toDouble()))
        }

        Gitt("at hundefører er ut av synet {string} minutter") { minutter: String ->
            opplysninger.leggTil(Faktum(BliPåPlassVilkår.minutterHundeførerHarVærtUtAvSynet, minutter.toDouble()))
        }

        Gitt("at øvelsen skal vare i {string} minutter") { minutter: String ->
            opplysninger.leggTil(Faktum(BliPåPlassVilkår.lengePåØvelse, minutter.toDouble()))
        }

        Gitt("at hunden ligger i {string} minutter") { minutter: String ->
            opplysninger.leggTil(Faktum(BliPåPlassVilkår.tidenHundenLigger, minutter.toDouble()))
        }

        Gitt("at oppgave til hundefører er gitt: {string}") { resultat: String ->
            opplysninger.leggTil(Faktum(BliPåPlassVilkår.oppgaveForHundeførerErGjennomført, resultat.toBoolean()))
        }

        Så("skal resultatet være {string}") { resultat: String ->
            val verdi =
                when (resultat) {
                    "Bestått" -> true
                    "Ikke bestått" -> false
                    else -> throw IllegalArgumentException("Ukjent utfall: $resultat")
                }

            opplysninger.har(BliPåPlassVilkår.bliPåPlassVilkår) shouldBe true
            opplysninger.finnOpplysning(BliPåPlassVilkår.bliPåPlassVilkår).verdi shouldBe verdi
        }
    }
}

private fun String.somLocalDate(): LocalDate {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return LocalDate.parse(this, formatter)
}
