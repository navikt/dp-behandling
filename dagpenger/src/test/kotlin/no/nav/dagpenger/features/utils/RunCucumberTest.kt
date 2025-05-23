package no.nav.dagpenger.features.utils

import io.cucumber.core.options.Constants.FILTER_TAGS_PROPERTY_NAME
import io.cucumber.core.options.Constants.PLUGIN_PROPERTY_NAME
import io.cucumber.core.options.Constants.PLUGIN_PUBLISH_ENABLED_PROPERTY_NAME
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.regel.Søknadstidspunkt
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite
import java.time.LocalDate

@Suite
@SelectClasspathResource("features")
@IncludeEngines("cucumber") //
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:build/reports/cucumber.html, no.nav.dagpenger.features.utils.RegeltreDokumentasjonPlugin",
)
@ConfigurationParameter(
    key = PLUGIN_PUBLISH_ENABLED_PROPERTY_NAME,
    value = "false",
)
@ConfigurationParameter(
    key = FILTER_TAGS_PROPERTY_NAME,
    value = "not @wip",
)
class RunCucumberTest

internal val opplysningerTilRegelkjøring: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger = {
    if (har(Søknadstidspunkt.prøvingsdato)) {
        forDato(finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi)
    } else {
        forDato(it)
    }
}
