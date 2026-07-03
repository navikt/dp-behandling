package no.nav.dagpenger.mediator.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.mediator.simulering.EvalueringsResultat
import no.nav.dagpenger.mediator.simulering.OpplysningNode
import no.nav.dagpenger.mediator.simulering.RegelsettRegister
import no.nav.dagpenger.mediator.simulering.SimuleringsEvaluering
import no.nav.dagpenger.mediator.simulering.UtledningNode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.Regelverk
import java.net.URI
import java.time.LocalDate

internal fun Application.generellSimuleringApi(register: RegelsettRegister) {
    val evaluering = SimuleringsEvaluering()

    routing {
        route("simulering") {
            get("regelverk") {
                val regelverk =
                    register.alleRegelverk().map { rv ->
                        mapOf(
                            "navn" to rv.navn.navn,
                            "href" to "/simulering/regelverk/${rv.navn.navn}",
                        )
                    }
                call.respond(HttpStatusCode.OK, regelverk)
            }

            route("regelverk/{regelverk}") {
                get {
                    val regelverkNavn = call.parameters["regelverk"]!!
                    val rv =
                        register.finnRegelverk(regelverkNavn)
                            ?: return@get call.respond(
                                HttpStatusCode.NotFound,
                                problemJson("Regelverk '$regelverkNavn' ikke funnet"),
                            )

                    val respons =
                        mapOf(
                            "navn" to rv.navn.navn,
                            "regelsett" to
                                rv.regelsett.map { rs ->
                                    mapOf(
                                        "navn" to rs.navn,
                                        "type" to rs.type.name,
                                        "href" to "/simulering/regelverk/$regelverkNavn/regelsett/${rs.navn}",
                                    )
                                },
                        )
                    call.respond(HttpStatusCode.OK, respons)
                }

                route("regelsett/{regelsett}") {
                    get {
                        val (rv, rs) = finnRegelsettEllerResponder(register) ?: return@get
                        call.respond(HttpStatusCode.OK, rs.tilSkjema(rv))
                    }

                    post("evaluer") {
                        val (rv, rs) = finnRegelsettEllerResponder(register) ?: return@post
                        val request = call.receive<EvaluerRequest>()

                        val resultat =
                            runCatching {
                                evaluering.evaluer(rs, request.dato, request.opplysninger.associate { it.behovId to it.verdi })
                            }.getOrElse { e ->
                                return@post call.respond(
                                    HttpStatusCode.BadRequest,
                                    problemJson("Evalueringsfeil: ${e.message}"),
                                )
                            }

                        call.respond(HttpStatusCode.OK, resultat.tilRespons())
                    }
                }
            }
        }
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.finnRegelsettEllerResponder(
    register: RegelsettRegister,
): Pair<Regelverk, Regelsett>? {
    val regelverkNavn = call.parameters["regelverk"]!!
    val regelsettNavn = call.parameters["regelsett"]!!
    return register.finnRegelsett(regelverkNavn, regelsettNavn)
        ?: run {
            call.respond(
                HttpStatusCode.NotFound,
                problemJson("Regelsett '$regelsettNavn' i regelverk '$regelverkNavn' ikke funnet"),
            )
            null
        }
}

private fun problemJson(
    detail: String,
    status: Int = 404,
) = mapOf(
    "type" to URI("urn:nav:no:dp:simulering:feil"),
    "title" to "Feil ved simulering",
    "status" to status,
    "detail" to detail,
)

private fun Regelsett.tilSkjema(rv: Regelverk): Map<String, Any> {
    val regelverkNavn = rv.navn.navn
    return mapOf(
        "navn" to navn,
        "hjemmel" to hjemmel.kortnavn,
        "type" to type.name,
        "inndata" to avhengerAv.map { it.tilSkjema() },
        "produserer" to produserer.map { it.tilSkjema() },
        "avhengigheterFor" to
            rv.avhengigheterFor(this).map { opp ->
                mapOf(
                    "navn" to opp.navn,
                    "href" to "/simulering/regelverk/$regelverkNavn/regelsett/${opp.navn}",
                    "kobler" to rv.grensesnittMellom(opp, this).map { it.behovId },
                )
            },
        "konsumenterAv" to
            rv.konsumenterAv(this).map { ned ->
                mapOf(
                    "navn" to ned.navn,
                    "href" to "/simulering/regelverk/$regelverkNavn/regelsett/${ned.navn}",
                    "kobler" to rv.grensesnittMellom(this, ned).map { it.behovId },
                )
            },
    )
}

private fun Opplysningstype<*>.tilSkjema(): Map<String, Any?> =
    mapOf(
        "id" to id.uuid,
        "navn" to navn,
        "behovId" to behovId,
        "datatype" to datatype.navn(),
        "formål" to formål.name,
        "enhet" to enhet?.name,
    )

private data class EvaluerRequest(
    val dato: LocalDate,
    val opplysninger: List<OpplysningInput>,
)

private data class OpplysningInput(
    val behovId: String,
    val verdi: Any?,
)

private fun EvalueringsResultat.tilRespons(): Map<String, Any> =
    mapOf(
        "opplysninger" to opplysninger.map { it.tilRespons() },
        "mangler" to mangler.toList(),
    )

private fun OpplysningNode.tilRespons(): Map<String, Any?> =
    mapOf(
        "navn" to navn,
        "behovId" to behovId,
        "datatype" to datatype,
        "verdi" to verdi,
        "utledetAv" to utledetAv?.tilRespons(),
    )

private fun UtledningNode.tilRespons(): Map<String, Any> =
    mapOf(
        "regel" to regel,
        "avhengigheter" to avhengigheter.map { it.tilRespons() },
    )
