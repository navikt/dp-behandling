package no.nav.dagpenger.features

import io.cucumber.java8.No
import io.kotest.matchers.equals.shouldBeEqual
import no.nav.dagpenger.dato.mai
import no.nav.dagpenger.features.utils.opplysningerTilRegelkjøring
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Opptjeningstid
import no.nav.dagpenger.regel.fastsetting.Søknadstidspunkt
import java.time.LocalDate

class OpptjeningstidSteg : No {
    private val forDato = 10.mai(2022)
    private val regelsett = listOf(Opptjeningstid.regelsett, Søknadstidspunkt.regelsett)
    private val opplysninger = Opplysninger()

    init {
        val regelkjøring =
            Regelkjøring(
                forDato,
                opplysninger,
                opplysningerTilRegelkjøring,
                *regelsett.toTypedArray(),
            )

        Gitt(
            "at søknadstidspunktet er {dato}",
        ) { søknadstidspunktet: LocalDate ->
            opplysninger
                .leggTil(
                    Faktum(
                        Søknadstidspunkt.søknadsdato,
                        søknadstidspunktet,
                    ),
                ).also { regelkjøring.evaluer() }
            opplysninger
                .leggTil(
                    Faktum(
                        Søknadstidspunkt.ønsketdato,
                        søknadstidspunktet,
                    ),
                ).also { regelkjøring.evaluer() }
        }
        Så(
            "er arbeidsgivers pliktige rapporteringsfrist {dato}",
        ) { rapporteringsfrist: LocalDate ->
            val faktiskRapporteringsfrist = opplysninger.finnOpplysning(Opptjeningstid.justertRapporteringsfrist)
            rapporteringsfrist shouldBeEqual faktiskRapporteringsfrist.verdi
        }
        Så(
            "opptjeningstiden er fra {dato}",
        ) { sisteAvsluttendeDato: LocalDate ->
            val faktiskSisteAvsluttendeDato = opplysninger.finnOpplysning(Opptjeningstid.sisteAvsluttendendeKalenderMåned)
            sisteAvsluttendeDato shouldBeEqual faktiskSisteAvsluttendeDato.verdi
        }
    }
}
