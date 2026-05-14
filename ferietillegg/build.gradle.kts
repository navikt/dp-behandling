
plugins {
    id("common")
    `java-library`
}
dependencies {
    implementation(project(path = ":opplysninger"))
    implementation(project(path = ":uuid-v7"))
    implementation(project(path = ":modell"))
    implementation(project(path = ":dato"))
    implementation(project(path = ":avklaring"))

    testImplementation(libs.kotest.assertions.core)
}
