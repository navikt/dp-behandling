plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(project(":dag"))
    api("com.fasterxml.uuid:java-uuid-generator:4.3.0")
    implementation("no.bekk.bekkopen:nocommons:0.16.0")
    api("org.javamoney:moneta:1.4.4")

    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.kotest.assertions.core)
}
