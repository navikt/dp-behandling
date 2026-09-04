plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(project(":opplysninger"))
    implementation(project(":avklaring"))
    implementation(project(":uuid-v7"))

    api("com.fasterxml.uuid:java-uuid-generator:5.2.0")
    api("no.nav.dagpenger:aktivitetslogg:20260903.45.ba6634")

    implementation(libs.kotlin.logging)
    testImplementation(libs.bundles.cucumber)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.bundles.jackson)
}
