
plugins {
    id("common")
    `java-library`
}
dependencies {
    api(project(path = ":opplysninger"))
    api(project(path = ":modell"))
    api(project(path = ":avklaring"))
    api(project(path = ":dato"))
    api(project(path = ":uuid-v7"))

    api(libs.rapids.and.rivers)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlin.logging)
}
