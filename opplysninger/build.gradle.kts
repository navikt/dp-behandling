plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(project(":dag"))
    implementation(project(":uuid-v7"))
    implementation("no.bekk.bekkopen:nocommons:0.16.0")
    api("com.github.navikt:dp-inntekt-kontrakter:1_20250716.854799")
    api("org.javamoney:moneta:1.4.4")
    api("no.nav.dagpenger:dp-grunnbelop:20250526.166.5b778c")
    api(libs.kotlin.logging)

    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.kotest.assertions.core)
}
