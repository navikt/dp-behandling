plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        // Fallback for OSS-koordinater mirror-en ikke har (f.eks. io.opentelemetry).
        // Uten denne feiler både Dependabot og bygg utenfor Nav-nettverket.
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20260728.276.7d515f")
            // om du vil teste nye versjoner før de merges til version-catalog, kan det gjøres slik:
            // version("ktor", "3.5.1")
        }
    }
}

rootProject.name = "dp-behandling"

include("dato")
include("modell")
include("openapi")
include("opplysninger")
include("dagpenger")
include("ferietillegg")
include("cucumber-testsupport")
include("utestengning")
include("regelverk")
include("mediator")
include("avklaring")
include("dag")
include("konfigurasjon")
include("uuid-v7")
