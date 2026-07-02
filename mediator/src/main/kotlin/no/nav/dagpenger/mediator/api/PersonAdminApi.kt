package no.nav.dagpenger.mediator.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.mediator.repository.PersonRepository
import no.nav.dagpenger.modell.Ident

internal fun Application.personAdminApi(personRepository: PersonRepository) {
    routing {
        authenticate("admin") {
            route("person/merge") {
                post {
                    val request = call.receive<MergePersonRequest>()
                    personRepository.merge(
                        winner = parseIdent(request.winnerIdent),
                        loser = parseIdent(request.loserIdent),
                    )
                    call.respond(HttpStatusCode.OK, MergePersonResponse())
                }
            }
            route("person/split") {
                post {
                    val request = call.receive<SplitPersonRequest>()
                    personRepository.split(
                        loserIdent = parseIdent(request.loserIdent),
                        fraIdent = parseIdent(request.fraIdent),
                    )
                    call.respond(HttpStatusCode.OK, SplitPersonResponse())
                }
            }
        }
    }
}

private fun parseIdent(ident: String): Ident =
    runCatching { Ident(ident) }.getOrElse {
        throw BadRequestException("Ugyldig ident – må være 11 siffer")
    }

internal data class MergePersonRequest(
    val winnerIdent: String,
    val loserIdent: String,
)

internal data class MergePersonResponse(
    val melding: String = "Merge fullført",
)

internal data class SplitPersonRequest(
    val loserIdent: String,
    val fraIdent: String,
)

internal data class SplitPersonResponse(
    val melding: String = "Split fullført",
)
