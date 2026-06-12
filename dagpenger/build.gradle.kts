
plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":regelverk"))
    implementation(project(path = ":dag"))
    implementation("no.nav.dagpenger:dp-grunnbelop:20260529.284.a0e9bd")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.1.0")
    implementation("io.opentelemetry:opentelemetry-api:1.36.0")
    testImplementation(project(path = ":cucumber-testsupport"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.bundles.jackson)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.rapids.and.rivers.test)
}
