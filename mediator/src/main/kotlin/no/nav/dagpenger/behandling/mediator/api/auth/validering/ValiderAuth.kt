package no.nav.dagpenger.behandling.mediator.api.auth.validering

import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import mu.KotlinLogging
import no.nav.dagpenger.behandling.konfigurasjon.Configuration
import no.nav.dagpenger.behandling.mediator.api.auth.saksbehandlerApp

private val logger = KotlinLogging.logger { }

internal fun JWTAuthenticationProvider.Config.autoriser() {
    val saksbehandlerGruppe = Configuration.properties[Configuration.Grupper.saksbehandler]
    val apper: List<String> = Configuration.properties[Configuration.Maskintilgang.navn]

    validate { jwtClaims ->
        val type = jwtClaims.payload.claims["idtype"]?.asString()
        logger.info("Tilgangsjekker idtype: $type")
        when (type) {
            "app" -> {
                jwtClaims.tilgangSjekkFor(apper)
            }
            else -> jwtClaims.måInneholde(ADGruppe = saksbehandlerGruppe)
        }
        JWTPrincipal(jwtClaims.payload)
    }
}

private fun JWTCredential.tilgangSjekkFor(apper: List<String>) =
    require(
        this.saksbehandlerApp().let { apper.contains(it) },
    ) {
        "Applikasjon mangler tilgang: ${this.saksbehandlerApp()}".also {
            logger.warn { it }
        }
    }

private fun JWTCredential.måInneholde(ADGruppe: String) =
    require(
        this.payload.claims["groups"]
            ?.asList(String::class.java)
            ?.contains(ADGruppe) ?: false,
    ) { "Mangler tilgang" }
