package no.nav.dagpenger.mediator.api.auth

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonProperty
import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.stringType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.konfigurasjon.Configuration
import no.nav.dagpenger.mediator.api.auth.validering.autoriser
import no.nav.dagpenger.mediator.api.auth.validering.autoriserAdminTilgang
import no.nav.dagpenger.mediator.objectMapper
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

object AuthFactory {
    @Suppress("ClassName")
    private object azure_app : PropertyGroup() {
        val well_known_url by stringType
        val client_id by stringType
    }

    private val azureAdConfiguration: OpenIdConfiguration by lazy {
        runBlocking {
            httpClient.get(Configuration.properties[azure_app.well_known_url]).body()
        }
    }

    enum class Issuer {
        AzureAD,
    }

    fun issuerFromString(issuer: String?) =
        when (issuer) {
            azureAdConfiguration.issuer -> Issuer.AzureAD
            else -> {
                throw IllegalArgumentException("Ikke støttet issuer: $issuer")
            }
        }

    fun JWTAuthenticationProvider.Config.azureAd() {
        realm = Configuration.APP_NAME
        verifiserTokenFormatOgSignatur()
        autoriser()
    }

    fun JWTAuthenticationProvider.Config.adminTilgang() {
        realm = Configuration.APP_NAME
        verifiserTokenFormatOgSignatur()
        autoriserAdminTilgang()
    }

    private fun JWTAuthenticationProvider.Config.verifiserTokenFormatOgSignatur() {
        verifier(
            jwkProvider = jwkProvider(URI(azureAdConfiguration.jwksUri).toURL()),
            issuer = azureAdConfiguration.issuer,
            configure = {
                withAudience(Configuration.properties[azure_app.client_id])
            },
        )
    }

    private fun jwkProvider(url: URL) =
        JwkProviderBuilder(url)
            .cached(10, 24, TimeUnit.HOURS) // cache up to 10 JWKs for 24 hours
            .rateLimited(
                10,
                1,
                TimeUnit.MINUTES,
            ) // if not cached, only allow max 10 different keys per minute to be fetched from external provider
            .build()
}

private data class OpenIdConfiguration(
    @param:JsonProperty("jwks_uri") val jwksUri: String,
    @param:JsonProperty("issuer") val issuer: String,
    @param:JsonProperty("token_endpoint") val tokenEndpoint: String,
    @param:JsonProperty("authorization_endpoint") val authorizationEndpoint: String,
)

private val httpClient =
    HttpClient(CIO) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }
    }
