
plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":regelverk"))
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.1.0")
    implementation("io.opentelemetry:opentelemetry-api:1.36.0")
    testImplementation(project(path = ":cucumber-testsupport"))
    testImplementation(libs.kotest.assertions.core)
}
