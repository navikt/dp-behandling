package no.nav.dagpenger.features

import io.cucumber.java.BeforeStep
import io.cucumber.java8.No
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.verdier.Beløp
import no.nav.dagpenger.regel.Minsteinntekt
import no.nav.dagpenger.regel.Opptjeningstid
import no.nav.dagpenger.regel.Rettighetstype
import no.nav.dagpenger.regel.Verneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt.prøvingsdato
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DagpengeperiodeSteg : No {
    private val fraDato = 10.mai(2022)
    private val regelsett =
        listOf(
            Dagpengegrunnlag.regelsett,
            Dagpengeperiode.regelsett,
            Minsteinntekt.regelsett,
            Opptjeningstid.regelsett,
            Rettighetstype.regelsett,
            Søknadstidspunkt.regelsett,
            Verneplikt.regelsett,
            VernepliktFastsetting.regelsett,
        )
    private val opplysninger: Opplysninger = Opplysninger()
    private lateinit var regelkjøring: Regelkjøring

    @BeforeStep
    fun kjørRegler() {
        regelkjøring = Regelkjøring(fraDato, opplysninger, *regelsett.toTypedArray())
    }

    init {

        Gitt("at søker har har rett til dagpenger fra {string}") { dato: String ->
            val dato = LocalDate.parse(dato, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            opplysninger.leggTil(Faktum(prøvingsdato, fraDato)).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum<LocalDate>(
                        prøvingsdato,
                        dato,
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }
        Gitt("at søker har {string} siste 12 måneder") { inntekt: String ->
            opplysninger
                .leggTil(
                    Faktum<Beløp>(
                        Minsteinntekt.inntekt12,
                        Beløp(inntekt.toBigDecimal()),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Gitt("at søker har {string} siste 36 måneder") { inntekt: String ->
            opplysninger
                .leggTil(
                    Faktum<Beløp>(
                        Minsteinntekt.inntekt36,
                        Beløp(inntekt.toBigDecimal()),
                    ) as Opplysning<*>,
                ).also { regelkjøring.evaluer() }
        }

        Så("skal søker ha {int} uker med dagpenger") { uker: Int ->
            val faktum = opplysninger.finnOpplysning(Dagpengeperiode.ordinærPeriode)
            faktum.verdi shouldBe uker
        }
    }
}
