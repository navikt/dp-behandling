package no.nav.dagpenger.mediator.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.allStatusCodes
import io.ktor.http.content.OutgoingContent
import io.ktor.http.isSuccess
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import no.nav.dagpenger.mediator.api.models.HttpProblemDTO
import java.net.URI

fun StatusPagesConfig.statusPagesConfig() {
    exception<BadRequestException> { call, cause ->
        call.application.log.warn("bad request: ${cause.message}. svarer med BadRequest og en feilmelding i JSON", cause)
        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            HttpStatusCode.BadRequest,
            HttpProblemDTO(
                title = "Ugyldig forespørsel",
                status = HttpStatusCode.BadRequest.value,
                type = URI("urn:error:bad_request"),
                detail = cause.message,
                instance = URI(call.request.uri),
            ),
        )
    }
    exception<NotFoundException> { call, cause ->
        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            HttpStatusCode.NotFound,
            HttpProblemDTO(
                status = HttpStatusCode.NotFound.value,
                title = "Ressurs ikke funnet",
                type = URI("urn:error:not_found"),
                detail = cause.message,
                instance = URI(call.request.uri),
            ),
        )
    }
    exception<Throwable> { call, cause ->
        call.application.log.error("ukjent feil: ${cause.message}. svarer med InternalServerError og en feilmelding i JSON", cause)
        call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
        call.respond(
            HttpStatusCode.InternalServerError,
            HttpProblemDTO(
                status = HttpStatusCode.InternalServerError.value,
                title = "Uventet feil",
                type = URI("urn:error:internal_error"),
                detail = "Uventet feil: ${cause.message}",
                instance = URI(call.request.uri),
            ),
        )
    }
    status(*allStatusCodes.filterNot { code -> code.isSuccess() }.toTypedArray()) { statusCode ->
        // exhaustive when-block so it will be compiler error if new types are added
        when (content) {
            is OutgoingContent.NoContent -> {
                call.response.header("Content-Type", ContentType.Application.ProblemJson.toString())
                call.respond(
                    statusCode,
                    HttpProblemDTO(
                        status = statusCode.value,
                        title = statusCode.description,
                        type = statusCode.toURI(call),
                        detail = statusCode.description,
                        instance = URI(call.request.uri),
                    ),
                )
            }

            is OutgoingContent.ByteArrayContent,
            is OutgoingContent.ContentWrapper,
            is OutgoingContent.ProtocolUpgrade,
            is OutgoingContent.ReadChannelContent,
            is OutgoingContent.WriteChannelContent,
            -> {
                // do nothing
            }
        }
    }
}

private fun HttpStatusCode.toURI(call: ApplicationCall): URI {
    val type =
        try {
            description.lowercase().replace("\\s+".toRegex(), "_")
        } catch (_: Exception) {
            call.application.log.error("klarte ikke lage uri fra httpstatuscode=$this")
            "unknown_error"
        }
    return URI("urn:error:$type")
}
