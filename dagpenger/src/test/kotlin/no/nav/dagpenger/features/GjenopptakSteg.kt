package no.nav.dagpenger.features

import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Gjenopptak
import no.nav.dagpenger.regel.Søknadstidspunkt
import no.nav.dagpenger.regel.beregning.Beregning
import java.time.LocalDate

class GjenopptakSteg : No {
    private val regelsett =
        listOf(Gjenopptak.regelsett, Søknadstidspunkt.regelsett, Beregning.regelsett)
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    init {
        Gitt("at søkeren har hatt en løpende stønadsperiode og har hatt minst en forbruksdag på {dato}") { sisteForbruksdag: LocalDate ->
            regelkjøring = Regelkjøring(sisteForbruksdag, opplysninger, *regelsett.toTypedArray())
            opplysninger.leggTil(Faktum(Beregning.forbrukt, 1, gyldighetsperiode = Gyldighetsperiode(sisteForbruksdag, sisteForbruksdag)))
        }

        Gitt("søker etter gjenopptak på {dato}") { gjenopptaksDato: LocalDate ->
            opplysninger.leggTil(Faktum(Søknadstidspunkt.prøvingsdato, gjenopptaksDato))
            regelkjøring.evaluer()
        }

        Så("skal gjenopptak være {boolsk}") { gjennoptak: Boolean ->
            val skalGjenoppta = opplysninger.finnOpplysning(Gjenopptak.skalGjenopptas)
            skalGjenoppta.verdi shouldBe gjennoptak
        }
    }
}
