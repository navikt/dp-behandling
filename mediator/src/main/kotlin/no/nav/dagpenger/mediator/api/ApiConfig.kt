package no.nav.dagpenger.mediator.api

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.jwt.jwt
import no.nav.dagpenger.mediator.api.auth.AuthFactory.adminTilgang
import no.nav.dagpenger.mediator.api.auth.AuthFactory.azureAd

internal fun Application.authenticationConfig(
    auth: AuthenticationConfig.() -> Unit = {
        jwt("azureAd") { azureAd() }
        jwt("admin") { adminTilgang() }
    },
) {
    install(Authentication) {
        auth()
    }
}
