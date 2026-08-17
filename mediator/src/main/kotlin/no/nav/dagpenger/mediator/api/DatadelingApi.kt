package no.nav.dagpenger.mediator.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.mediator.api.models.DatadelingForesporselDTO
import no.nav.dagpenger.mediator.datadeling.DatadelingService

/**
 * Datadelings-API som svarer direkte fra dp-behandlings database.
 *
 * Kontrakten er definert i behandling-api.yaml. Hvilken kilde som skal
 * brukes (dette API-et eller dp-datadelings egen database) styres med
 * Unleash-toggle i dp-datadeling.
 */
internal fun Application.datadelingApi(datadelingService: DatadelingService) {
    routing {
        authenticate("azureAd") {
            route("/dagpenger/datadeling/v1") {
                post("/perioder") {
                    val forespørsel = call.receive<DatadelingForesporselDTO>()
                    call.respond(HttpStatusCode.OK, datadelingService.hentPerioder(forespørsel))
                }
                post("/beregninger") {
                    val forespørsel = call.receive<DatadelingForesporselDTO>()
                    call.respond(HttpStatusCode.OK, datadelingService.hentBeregninger(forespørsel))
                }
            }
        }
    }
}
