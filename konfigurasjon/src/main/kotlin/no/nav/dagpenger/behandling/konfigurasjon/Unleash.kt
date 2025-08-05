package no.nav.dagpenger.behandling.konfigurasjon

import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import no.nav.dagpenger.behandling.konfigurasjon.Configuration.properties
import java.net.InetAddress

val unleash: Unleash =
    if (properties.getOrNull(Key("UNLEASH_SERVER_API_URL", stringType)) == null) {
        FakeUnleash()
    } else {
        DefaultUnleash(
            UnleashConfig
                .builder()
                .appName(properties[Key("NAIS_APP_NAME", stringType)])
                .instanceId(runCatching { InetAddress.getLocalHost().hostName }.getOrElse { "ukjent" })
                .unleashAPI(properties[Key("UNLEASH_SERVER_API_URL", stringType)] + "/api/")
                .apiKey(properties[Key("UNLEASH_SERVER_API_TOKEN", stringType)])
                .environment(
                    when (System.getenv("NAIS_CLUSTER_NAME").orEmpty()) {
                        "prod-gcp" -> "production"
                        else -> "development"
                    },
                ).build(),
        )
    }

fun skruPåFeature(feature: Feature) {
    require(unleash is FakeUnleash)
    unleash.enable(feature.navn)
}

fun skruAvFeatures() {
    require(unleash is FakeUnleash)
    unleash.resetAll()
}

enum class Feature(
    val navn: String,
) {
    KJEDING_AV_BEHANDLING("dp-behandling.kjeding"),
    REDIGERING_AV_REGISTRERT_ARBEIDSSØKER("dp-behandling.redigering-arbeidssøker"),
}
