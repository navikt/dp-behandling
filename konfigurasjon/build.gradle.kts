plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
    api("io.getunleash:unleash-client-java:9.3.2")
}
