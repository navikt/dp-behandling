plugins {
    id("common")
    `java-library`
}

dependencies {
    api("com.fasterxml.uuid:java-uuid-generator:5.2.0")
    testImplementation(libs.kotest.assertions.core)
}
