package no.nav.dagpenger.behandling.mediator.api.auth.validering

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal
import no.nav.dagpenger.behandling.konfigurasjon.Configuration
import no.nav.dagpenger.behandling.mediator.api.auth.saksbehandlerApp

private val logger = KotlinLogging.logger { }

internal fun JWTAuthenticationProvider.Config.autoriser() {
    val saksbehandlerGruppe = Configuration.properties[Configuration.Grupper.saksbehandler]
    val apper: List<String> = Configuration.properties[Configuration.Maskintilgang.navn]

    validate { jwtClaims: JWTCredential ->
        val type = jwtClaims.payload.claims["idtyp"]?.asString()
        logger.trace { "Tilgangsjekker idtyp: $type" }
        when (type) {
            "app" -> {
                jwtClaims.tilgangsjekkForMaskinToken(apper)
            }
            else -> jwtClaims.tilgangsjekkForSaksbehandler(ADGruppe = saksbehandlerGruppe)
        }
        JWTPrincipal(jwtClaims.payload)
    }
}

private fun JWTCredential.tilgangsjekkForMaskinToken(apper: List<String>) =
    require(
        this.saksbehandlerApp().let { apper.contains(it) },
    ) {
        "Applikasjon mangler tilgang: ${this.saksbehandlerApp()}".also {
            logger.warn { it }
        }
    }

private fun JWTCredential.tilgangsjekkForSaksbehandler(ADGruppe: String) =
    require(
        this.payload.claims["groups"]
            ?.asList(String::class.java)
            ?.contains(ADGruppe) ?: false,
    ) { "Mangler tilgang" }

internal fun JWTAuthenticationProvider.Config.autoriserAdminTilgang() {
    val adminGruppe = Configuration.properties[Configuration.Grupper.admin]

    validate { jwtClaims: JWTCredential ->
        jwtClaims.måInneholdeAdminTilgang(adminGruppe = adminGruppe)
        JWTPrincipal(jwtClaims.payload)
    }
}

private fun JWTCredential.måInneholdeAdminTilgang(adminGruppe: String) {
    require(
        this.payload.claims["groups"]
            ?.asList(String::class.java)
            ?.contains(adminGruppe) ?: false,
    ) { "Mangler admin tilgang" }
}
