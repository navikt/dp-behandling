package no.nav.dagpenger.behandling.mediator.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.github.navikt.tbd_libs.naisful.test.TestContext
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.server.application.Application
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.dagpenger.behandling.konfigurasjon.Configuration
import no.nav.dagpenger.behandling.objectMapper
import no.nav.security.mock.oauth2.MockOAuth2Server

object TestApplication {
    private const val AZUREAD_ISSUER_ID = "azureAd"
    private const val CLIENT_ID = "dp-soknad"

    private val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { server ->
            server.start()
        }
    }

    internal fun testAzureAdToken(ADGrupper: List<String>): String =
        mockOAuth2Server
            .issueToken(
                issuerId = AZUREAD_ISSUER_ID,
                audience = CLIENT_ID,
                claims =
                    mapOf(
                        "NAVident" to "123",
                        "groups" to ADGrupper,
                    ),
            ).serialize()

    internal fun withMockAuthServerAndTestApplication(
        moduleFunction: Application.() -> Unit,
        test: suspend TestContext.() -> Unit,
    ) {
        System.setProperty("azure-app.client-id", CLIENT_ID)
        System.setProperty("azure-app.well-known-url", "${mockOAuth2Server.wellKnownUrl(AZUREAD_ISSUER_ID)}")

        return naisfulTestApp(
            {
                apply { moduleFunction() }
            },
            objectMapper.apply {
                // OpenAPI-generator klarer ikke optional-felter. Derfor må vi eksplisitt fjerne null-verdier
                setSerializationInclusion(JsonInclude.Include.NON_NULL)
            },
            PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        ) {
            test()
        }
    }

    internal suspend fun TestContext.autentisert(
        endepunkt: String,
        token: String =
            testAzureAdToken(
                ADGrupper = listOf(Configuration.properties[Configuration.Grupper.saksbehandler]),
            ),
        httpMethod: HttpMethod = HttpMethod.Post,
        body: String? = null,
    ): HttpResponse =
        client.request(endepunkt) {
            this.method = httpMethod
            body?.let { this.setBody(TextContent(it, ContentType.Application.Json)) }
            this.header(HttpHeaders.Authorization, "Bearer $token")
            this.header(HttpHeaders.Accept, ContentType.Application.Json.toString())
            this.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        }
}
