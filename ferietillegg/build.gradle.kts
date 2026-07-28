
plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":regelverk"))
    implementation(libs.otel.instrumentation.annotations)
    implementation(libs.otel.api)
    testImplementation(project(path = ":cucumber-testsupport"))
    testImplementation(libs.kotest.assertions.core)
}
