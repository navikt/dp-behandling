package no.nav.dagpenger.features.utils

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.regel.Søknadstidspunkt
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite
import java.time.LocalDate

// Konfigurasjon av cucumber ligger i src/test/resources/junit-platform.properties
@Suite
@SelectClasspathResource("features")
@IncludeEngines("cucumber")
class RunCucumberTest

internal val opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger = {
    if (har(Søknadstidspunkt.prøvingsdato)) {
        forDato(finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi)
    } else {
        forDato(it)
    }
}
