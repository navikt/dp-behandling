plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20251028.226.3bba2c")
        }
    }
}

rootProject.name = "dp-behandling"

include("dato")
include("modell")
include("openapi")
include("opplysninger")
include("dagpenger")
include("mediator")
include("avklaring")
include("dag")
include("konfigurasjon")
include("uuid-v7")
