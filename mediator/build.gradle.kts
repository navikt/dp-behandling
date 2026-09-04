plugins {
    id("common")
    application
    kotlin("plugin.serialization") version "2.4.10"
}

dependencies {
    implementation(project(path = ":konfigurasjon"))
    implementation(project(path = ":regelverk"))
    implementation(project(path = ":modell"))
    implementation(project(path = ":openapi"))
    implementation(project(path = ":dagpenger"))
    implementation(project(path = ":ferietillegg"))
    implementation(project(path = ":utestengning"))
    implementation(project(path = ":opplysninger"))
    implementation(project(path = ":avklaring"))
    implementation(project(path = ":uuid-v7"))
    implementation(project(path = ":dato"))

    implementation(libs.bundles.jackson)

    implementation(libs.bundles.postgres)
    implementation("tools.jackson.module:jackson-module-blackbird:${libs.versions.jackson.get()}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.11.0")

    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
    implementation(libs.otel.instrumentation.annotations)
    implementation(libs.otel.api)
    implementation("io.prometheus:prometheus-metrics-core:1.8.0")

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)
    implementation("io.ktor:ktor-server-core-jvm:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-swagger:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-status-pages:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-serialization-jackson3:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-sse:${libs.versions.ktor.get()}")

    testImplementation(libs.kotest.assertions.core)

    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.bundles.postgres.test)
    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktor.get()}")
    testImplementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")
    testImplementation("io.ktor:ktor-serialization-jackson3:${libs.versions.ktor.get()}")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")
    testImplementation(libs.approvaltests)
}

application {
    mainClass.set("no.nav.dagpenger.mediator.AppKt")
}

tasks.test {
    val erCI = System.getenv("CI")?.toBoolean() == true
    val defaultParallelism = if (erCI) 1 else 8
    val parallelism = System.getenv("TEST_PARALLELISM")?.toInt() ?: defaultParallelism
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", parallelism)
    systemProperty("junit.jupiter.execution.parallel.config.fixed.max-pool-size", parallelism)
}
