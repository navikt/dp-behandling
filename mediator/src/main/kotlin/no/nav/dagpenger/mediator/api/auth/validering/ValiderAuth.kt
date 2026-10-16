package no.nav.dagpenger.mediator.api.auth.validering

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.JWTPrincipal

private val logger = KotlinLogging.logger { }

internal fun JWTAuthenticationProvider.Config.autoriser(saksbehandlerGruppe: String) {
    validate { jwtClaims: JWTCredential ->
        val type = jwtClaims.payload.claims["idtyp"]?.asString()
        logger.trace { "Tilgangsjekker idtyp: $type" }
        when (type) {
            "app" -> {
                jwtClaims.tilgangsjekkForMaskinToken()
            }

            else -> {
                jwtClaims.tilgangsjekkForSaksbehandler(ADGruppe = saksbehandlerGruppe)
            }
        }
        JWTPrincipal(jwtClaims.payload)
    }
}

/**
 * Entra ID utsteder kun maskintoken for vår audience til apper som er
 * eksplisitt godkjent i accessPolicy.inbound.rules i nais.yaml - slike apper
 * får alltid standardrollen access_as_application i roles-claimet. Dette gjør
 * accessPolicy den eneste kilden til sannhet for hvilke apper som har tilgang,
 * fremfor en egen, manuelt vedlikeholdt liste med app-navn (som i tillegg
 * risikerte å komme i utakt med accessPolicy).
 *
 * NB: azp_name er ikke egnet til autorisering (Nais sin dokumentasjon sier
 * eksplisitt at det ikke er garantert unikt og ikke skal brukes til dette).
 */
private fun JWTCredential.tilgangsjekkForMaskinToken() =
    require(
        this.payload.claims["roles"]
            ?.asList(String::class.java)
            ?.contains("access_as_application") ?: false,
    ) { "Maskintoken mangler forventet rolle access_as_application".also { logger.warn { it } } }

private fun JWTCredential.tilgangsjekkForSaksbehandler(ADGruppe: String) =
    require(
        this.payload.claims["groups"]
            ?.asList(String::class.java)
            ?.contains(ADGruppe) ?: false,
    ) { "Mangler tilgang" }

internal fun JWTAuthenticationProvider.Config.autoriserAdminTilgang(adminGrupper: List<String>) {
    validate { jwtClaims: JWTCredential ->
        jwtClaims.måInneholdeAdminTilgang(adminGrupper = adminGrupper)
        JWTPrincipal(jwtClaims.payload)
    }
}

private fun JWTCredential.måInneholdeAdminTilgang(adminGrupper: List<String>) {
    val brukerGrupper = this.payload.claims["groups"]?.asList(String::class.java) ?: emptyList()
    require(brukerGrupper.any { it in adminGrupper }) {
        "Mangler admin tilgang".also {
            logger.warn { it }
        }
    }
}
