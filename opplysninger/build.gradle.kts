plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(project(":dag"))
    implementation(project(":dato"))
    implementation(project(":uuid-v7"))
    api("com.github.navikt:dp-inntekt-kontrakter:2_20251211.17f9d7")
    api("org.javamoney:moneta:1.4.4")
    api(libs.dp.grunnbelop)
    api(libs.kotlin.logging)
    api("no.nav.dagpenger:aktivitetslogg:20251016.40.a3c526")
    implementation(libs.otel.instrumentation.annotations)
    implementation(libs.otel.api)

    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.kotest.assertions.core)
}
