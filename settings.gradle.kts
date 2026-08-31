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
            from("no.nav.dagpenger:dp-version-catalog:20260831.284")
            // om du vil teste nye versjoner før de merges til version-catalog, kan det gjøres slik:
            // version("ktor", "3.5.1")

            // Avhengigheter som ikke (enda) finnes i dp-version-catalog, men som brukes i flere
            // moduler her. Definert sentralt for å unngå at modulene drifter fra hverandre.
            version("otel-api", "1.65.0")
            version("otel-instrumentation", "2.30.0")
            library("otel-api", "io.opentelemetry", "opentelemetry-api").versionRef("otel-api")
            library(
                "otel-instrumentation-annotations",
                "io.opentelemetry.instrumentation",
                "opentelemetry-instrumentation-annotations",
            ).versionRef("otel-instrumentation")
            library("dp-grunnbelop", "no.nav.dagpenger", "dp-grunnbelop").version("20260529.285.e99922")
            library("approvaltests", "com.approvaltests", "approvaltests").version("31.0.0")
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
