plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(project(":dag"))
    implementation(project(":dato"))
    implementation(project(":uuid-v7"))
    api("com.github.navikt:dp-inntekt-kontrakter:2_202609181789745424.d9cfdc")
    api("org.javamoney:moneta:1.4.5")
    api(libs.dp.grunnbelop)
    api(libs.kotlin.logging)
    api("no.nav.dagpenger:aktivitetslogg:20261028.49.ded0d7")
    implementation(libs.otel.instrumentation.annotations)
    implementation(libs.otel.api)

    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.kotest.assertions.core)
}
