package no.nav.dagpenger.features

import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@SelectClasspathResource("features")
@IncludeEngines("cucumber")
class RunCucumberTest
