plugins {
    id("common")
    `java-library`
}

val cucumberVersion = "7.15.0"
dependencies {
    implementation(project(":opplysninger"))
    implementation(project(":avklaring"))
    implementation(project(":uuid-v7"))

    api("com.fasterxml.uuid:java-uuid-generator:4.3.0")
    api("no.nav.dagpenger:aktivitetslogg:20250624.31.bf07ce")

    implementation(libs.kotlin.logging)
    testImplementation("io.cucumber:cucumber-java8:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testImplementation(libs.kotest.assertions.core)
    testImplementation("org.junit.platform:junit-platform-suite:1.10.2")
    testImplementation(libs.bundles.jackson)
}
