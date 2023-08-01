plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(project(path = ":aktivitetslogg"))
    api(libs.type.id)
    testImplementation("io.cucumber:cucumber-java8:${libs.versions.cucumber.get()}")
    testImplementation("io.cucumber:cucumber-junit-platform-engine:${libs.versions.cucumber.get()}")
    testImplementation("io.kotest:kotest-assertions-core-jvm:${libs.versions.kotest.get()}")
    testImplementation("org.junit.platform:junit-platform-suite:${libs.versions.junit.platform.suite.get()}")
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.jackson.module.kotlin)
}
