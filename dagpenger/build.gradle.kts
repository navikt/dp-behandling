
plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":regelverk"))
    implementation(project(path = ":dag"))
    implementation(libs.dp.grunnbelop)
    implementation(libs.otel.instrumentation.annotations)
    implementation(libs.otel.api)
    testImplementation(project(path = ":cucumber-testsupport"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.bundles.jackson)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.rapids.and.rivers.test)
}

tasks.test {
    // cucumber.plugin (satt i junit-platform.properties) skriver denne HTML-rapporten som en
    // side-effekt av testkjøringen. Den må deklareres som en output for at Gradle build cache
    // skal gjenopprette filen ved cache-treff – ellers forsvinner den etter en `clean`-bygg.
    outputs
        .file(layout.buildDirectory.file("reports/cucumber.html"))
        .withPropertyName("cucumberHtmlReport")
}
