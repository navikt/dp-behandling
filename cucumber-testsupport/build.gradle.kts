plugins {
    id("common")
    `java-library`
}

dependencies {
    api(project(path = ":opplysninger"))
    implementation(project(path = ":dag"))

    api("io.cucumber:cucumber-java:7.22.1")
    api("io.cucumber:cucumber-java8:7.22.1")
    api("io.cucumber:cucumber-junit-platform-engine:7.22.1")
    api("org.junit.platform:junit-platform-suite:1.10.2")
    api("com.approvaltests:approvaltests:22.3.3")
}
