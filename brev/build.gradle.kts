plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(project(":openapi"))
    implementation(libs.bundles.jackson)

    testImplementation(libs.kotest.assertions.core)
}
