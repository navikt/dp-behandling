
plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":regelverk"))
    implementation(project(path = ":dag"))
    implementation(libs.dp.grunnbelop)
    implementation(libs.otel.instrumentation.annotations)
    implementation(libs.otel.api)
    testImplementation(project(path = ":cucumber-testsupport"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.bundles.jackson)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
    testImplementation(libs.rapids.and.rivers.test)
}
