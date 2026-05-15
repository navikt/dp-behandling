
plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":regelverk"))
    implementation(project(path = ":dag"))
    implementation("no.nav.dagpenger:dp-grunnbelop:20260109.219.701e55")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.1.0")
    implementation("io.opentelemetry:opentelemetry-api:1.36.0")

    testImplementation("io.cucumber:cucumber-java:7.22.1")
    testImplementation("io.cucumber:cucumber-java8:7.22.1")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:7.22.1")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.2")
    testImplementation("com.approvaltests:approvaltests:22.3.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.bundles.jackson)
    testImplementation(libs.kotest.assertions.core)
}
