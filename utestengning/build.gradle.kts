plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":regelverk"))

    testImplementation(libs.kotest.assertions.core)
}
