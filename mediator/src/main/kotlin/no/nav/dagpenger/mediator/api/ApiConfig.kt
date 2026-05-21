package no.nav.dagpenger.mediator.api

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import no.nav.dagpenger.mediator.api.auth.AuthFactory

internal fun Application.authenticationConfig(authFactory: AuthFactory) {
    install(Authentication) {
        jwt("azureAd") {
            with(authFactory) {
                azureAd()
            }
        }
        jwt("admin") {
            with(authFactory) {
                adminTilgang()
            }
        }
    }
}
