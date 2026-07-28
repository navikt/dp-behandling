plugins {
    id("common")
    `java-library`
}

dependencies {
    api(project(path = ":opplysninger"))
    implementation(project(path = ":dag"))

    api(libs.bundles.cucumber)
    api(libs.approvaltests)
}
