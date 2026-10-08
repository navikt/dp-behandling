plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":regelverk"))
    implementation(libs.otel.instrumentation.annotations)

    testImplementation(libs.kotest.assertions.core)
}
