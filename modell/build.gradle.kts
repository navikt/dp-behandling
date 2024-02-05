plugins {
    id("common")
    `java-library`
}

val cucumberVersion = "7.15.0"
dependencies {
    api(libs.dp.aktivitetslogg)
    implementation(libs.kotlin.logging)
    testImplementation("io.cucumber:cucumber-java8:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:$cucumberVersion")
    testImplementation(libs.kotest.assertions.core)
    testImplementation("org.junit.platform:junit-platform-suite:1.10.2")
    testImplementation(libs.bundles.jackson)
}
