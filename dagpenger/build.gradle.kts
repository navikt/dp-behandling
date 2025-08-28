
plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":dag"))
    implementation(project(path = ":opplysninger"))
    implementation(project(path = ":uuid-v7"))
    implementation(project(path = ":modell"))
    implementation(project(path = ":dato"))
    implementation(project(path = ":avklaring"))
    implementation(project(path = ":konfigurasjon"))
    implementation("no.nav.dagpenger:dp-grunnbelop:20250422.156.910f4f")

    testImplementation("io.cucumber:cucumber-java:7.22.1")
    testImplementation("io.cucumber:cucumber-java8:7.22.1")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.22.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.2")
    testImplementation("com.approvaltests:approvaltests:22.3.3")
    testImplementation(libs.bundles.jackson)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
    // Generer både HTML og JSON (JSON er nyttig til videre prosessering)
    systemProperty(
        "cucumber.plugin",
        "pretty, html:build/reports/cucumber.html, no.nav.dagpenger.features.utils.RegeltreDokumentasjonPlugin",
    )
    systemProperty("cucumber.publish.quiet", "true")
}
