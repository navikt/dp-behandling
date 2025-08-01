package no.nav.dagpenger.behandling.mediator.api.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPayloadHolder
import io.ktor.server.auth.jwt.JWTPrincipal

internal fun ApplicationCall.saksbehandlerId() =
    requireNotNull(this.authentication.principal<JWTPrincipal>()) { "Ikke autentisert" }.saksbehandlerId()

internal fun ApplicationCall.saksbehandlerIdOrNull(): String? =
    runCatching { this.authentication.principal<JWTPrincipal>()?.saksbehandlerId() }.getOrNull()

internal fun JWTPayloadHolder.saksbehandlerId(): String = requireNotNull(this.payload.claims["NAVident"]?.asString())

internal fun JWTPayloadHolder.saksbehandlerApp(): String = requireNotNull(this.payload.claims["azp_name"]?.asString())
