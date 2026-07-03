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
import no.nav.dagpenger.mediator.simulering.api.models.EvaluerRequestDTO
import no.nav.dagpenger.mediator.simulering.api.models.EvaluerResultatDTO
import no.nav.dagpenger.mediator.simulering.api.models.HttpProblemDTO
import no.nav.dagpenger.mediator.simulering.api.models.OpplysningNodeDTO
import no.nav.dagpenger.mediator.simulering.api.models.OpplysningstypeSkjemaDTO
import no.nav.dagpenger.mediator.simulering.api.models.OpplysningstypeSkjemaDTODatatypeDTO
import no.nav.dagpenger.mediator.simulering.api.models.OpplysningstypeSkjemaDTOFormålDTO
import no.nav.dagpenger.mediator.simulering.api.models.RegelsettKoblingDTO
import no.nav.dagpenger.mediator.simulering.api.models.RegelsettRefDTO
import no.nav.dagpenger.mediator.simulering.api.models.RegelsettRefDTOTypeDTO
import no.nav.dagpenger.mediator.simulering.api.models.RegelsettSkjemaDTO
import no.nav.dagpenger.mediator.simulering.api.models.RegelsettSkjemaDTOTypeDTO
import no.nav.dagpenger.mediator.simulering.api.models.RegelverkGrafDTO
import no.nav.dagpenger.mediator.simulering.api.models.RegelverkOversiktDTO
import no.nav.dagpenger.mediator.simulering.api.models.UtledningDTO
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.Regelverk
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun Application.generellSimuleringApi(register: RegelsettRegister) {
    val evaluering = SimuleringsEvaluering()

    routing {
        route("simulering") {
            get("regelverk") {
                val regelverk =
                    register.alleRegelverk().map { rv ->
                        RegelverkOversiktDTO(
                            navn = rv.navn.navn,
                            href = URI("/simulering/regelverk/${rv.navn.navn.urlEnkod()}"),
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
                        RegelverkGrafDTO(
                            navn = rv.navn.navn,
                            regelsett =
                                rv.regelsett.map { rs ->
                                    RegelsettRefDTO(
                                        navn = rs.navn,
                                        type = RegelsettRefDTOTypeDTO.fromValue(rs.type.name),
                                        href = URI("/simulering/regelverk/$regelverkNavn/regelsett/${rs.navn.urlEnkod()}"),
                                    )
                                },
                        )
                    call.respond(HttpStatusCode.OK, respons)
                }

                route("regelsett/{regelsett}") {
                    get {
                        val (rv, rs) = finnRegelsettEllerResponder(register) ?: return@get
                        call.respond(HttpStatusCode.OK, rs.tilSkjemaDTO(rv))
                    }

                    post("evaluer") {
                        val (_, rs) = finnRegelsettEllerResponder(register) ?: return@post
                        val request = call.receive<EvaluerRequestDTO>()

                        val resultat =
                            runCatching {
                                evaluering.evaluer(rs, request.dato, request.opplysninger.associate { it.behovId to it.verdi })
                            }.getOrElse { e ->
                                return@post call.respond(
                                    HttpStatusCode.BadRequest,
                                    problemJson("Evalueringsfeil: ${e.message}", status = 400),
                                )
                            }

                        call.respond(HttpStatusCode.OK, resultat.tilDTO())
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
    val regelsettNavn = call.parameters["regelsett"]!!.urlDekod()
    return register.finnRegelsett(regelverkNavn, regelsettNavn)
        ?: run {
            call.respond(
                HttpStatusCode.NotFound,
                problemJson("Regelsett '$regelsettNavn' i regelverk '$regelverkNavn' ikke funnet"),
            )
            null
        }
}

private fun String.urlEnkod() = URLEncoder.encode(this, StandardCharsets.UTF_8)

private fun String.urlDekod() = java.net.URLDecoder.decode(this, StandardCharsets.UTF_8)

private fun problemJson(
    detail: String,
    status: Int = 404,
) = HttpProblemDTO(
    type = URI("urn:nav:no:dp:simulering:feil"),
    title = "Feil ved simulering",
    status = status,
    detail = detail,
)

private fun Regelsett.tilSkjemaDTO(rv: Regelverk): RegelsettSkjemaDTO {
    val regelverkNavn = rv.navn.navn
    return RegelsettSkjemaDTO(
        navn = navn,
        hjemmel = hjemmel.kortnavn,
        type = RegelsettSkjemaDTOTypeDTO.fromValue(type.name)!!,
        inndata = (avhengerAv + behov).map { it.tilDTO() },
        produserer = produserer.map { it.tilDTO() },
        avhengigheterFor =
            rv.avhengigheterFor(this).map { opp ->
                RegelsettKoblingDTO(
                    navn = opp.navn,
                    href = URI("/simulering/regelverk/$regelverkNavn/regelsett/${opp.navn.urlEnkod()}"),
                    kobler = rv.grensesnittMellom(opp, this).map { it.behovId },
                )
            },
        konsumenterAv =
            rv.konsumenterAv(this).map { ned ->
                RegelsettKoblingDTO(
                    navn = ned.navn,
                    href = URI("/simulering/regelverk/$regelverkNavn/regelsett/${ned.navn.urlEnkod()}"),
                    kobler = rv.grensesnittMellom(this, ned).map { it.behovId },
                )
            },
    )
}

private fun Opplysningstype<*>.tilDTO(): OpplysningstypeSkjemaDTO =
    OpplysningstypeSkjemaDTO(
        id = id.uuid,
        navn = navn,
        behovId = behovId,
        datatype = OpplysningstypeSkjemaDTODatatypeDTO.fromValue(datatype.navn())!!,
        formål = OpplysningstypeSkjemaDTOFormålDTO.fromValue(formål.name)!!,
        enhet = enhet?.name,
    )

private fun EvalueringsResultat.tilDTO(): EvaluerResultatDTO =
    EvaluerResultatDTO(
        opplysninger = opplysninger.map { it.tilDTO() },
        mangler = mangler.toList(),
    )

private fun OpplysningNode.tilDTO(): OpplysningNodeDTO =
    OpplysningNodeDTO(
        navn = navn,
        behovId = behovId,
        datatype = datatype,
        verdi = verdi,
        utledetAv = utledetAv?.tilDTO(),
    )

private fun UtledningNode.tilDTO(): UtledningDTO =
    UtledningDTO(
        regel = regel,
        avhengigheter = avhengigheter.map { it.tilDTO() },
    )
