package no.nav.dagpenger.behandling.mediator.api

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.opentelemetry.api.trace.Span
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.aktivitetslogg.AuditOperasjon
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.IdentForesporselDTO
import no.nav.dagpenger.behandling.api.models.KvitterAvklaringRequestDTO
import no.nav.dagpenger.behandling.api.models.KvitteringDTO
import no.nav.dagpenger.behandling.api.models.OppdaterOpplysningRequestDTO
import no.nav.dagpenger.behandling.api.models.OpplysningstypeDTO
import no.nav.dagpenger.behandling.konfigurasjon.støtterInnvilgelse
import no.nav.dagpenger.behandling.mediator.IHendelseMediator
import no.nav.dagpenger.behandling.mediator.OpplysningSvarBygger.VerdiMapper
import no.nav.dagpenger.behandling.mediator.api.auth.saksbehandlerId
import no.nav.dagpenger.behandling.mediator.audit.Auditlogg
import no.nav.dagpenger.behandling.mediator.lagVedtak
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.ForslagTilVedtak
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.Redigert
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.TilBeslutning
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.TilGodkjenning
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger { }

internal fun Application.behandlingApi(
    personRepository: PersonRepository,
    hendelseMediator: IHendelseMediator,
    auditlogg: Auditlogg,
    opplysningstyper: Set<Opplysningstype<*>>,
    messageContext: (ident: String) -> MessageContext,
) {
    authenticationConfig()
    install(OtelTraceIdPlugin)

    routing {
        swaggerUI(path = "openapi", swaggerFile = "behandling-api.yaml")

        get("/") { call.respond(HttpStatusCode.OK) }
        get("/features") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "støtterInnvilgelse" to støtterInnvilgelse,
                ),
            )
        }
        get("/opplysningstyper") {
            val typer =
                opplysningstyper.map {
                    OpplysningstypeDTO(
                        it.id,
                        it.navn,
                        when (it.datatype) {
                            Boolsk -> DataTypeDTO.boolsk
                            Dato -> DataTypeDTO.dato
                            Desimaltall -> DataTypeDTO.desimaltall
                            Heltall -> DataTypeDTO.heltall
                            ULID -> DataTypeDTO.ulid
                            Penger -> DataTypeDTO.penger
                            InntektDataType -> DataTypeDTO.inntekt
                            BarnDatatype -> DataTypeDTO.barn
                            Tekst -> DataTypeDTO.tekst
                        },
                    )
                }
            call.respond(HttpStatusCode.OK, typer)
        }

        authenticate("azureAd") {
            route("behandling") {
                post {
                    val identForespørsel = call.receive<IdentForesporselDTO>()
                    val ident = identForespørsel.ident
                    val person =
                        personRepository.hent(
                            ident.tilPersonIdentfikator(),
                        ) ?: throw NotFoundException("Person ikke funnet")

                    auditlogg.les("Listet ut behandlinger", ident, call.saksbehandlerId())

                    call.respond(HttpStatusCode.OK, person.behandlinger().map { it.tilBehandlingDTO() })
                }

                route("{behandlingId}") {
                    get {
                        val behandling = hentBehandling(personRepository, call.behandlingId)

                        auditlogg.les("Så en behandling", behandling.behandler.ident, call.saksbehandlerId())

                        call.respond(HttpStatusCode.OK, behandling.tilBehandlingDTO())
                    }

                    get("vedtak") {
                        val behandling = hentBehandling(personRepository, call.behandlingId)

                        auditlogg.les("Så en behandling", behandling.behandler.ident, call.saksbehandlerId())

                        call.respond(
                            HttpStatusCode.OK,
                            lagVedtak(
                                behandling.behandlingId,
                                Ident(behandling.behandler.ident),
                                behandling.behandler.eksternId,
                                behandling.opplysninger(),
                                behandling.erAutomatiskBehandlet(),
                                behandling.godkjent,
                                behandling.besluttet,
                            ),
                        )
                    }

                    post("godkjenn") {
                        val identForespørsel = call.receive<IdentForesporselDTO>()
                        val behandling = hentBehandling(personRepository, call.behandlingId)

                        // TODO: La dette egentlig komme fra modellen
                        if (!behandling.harTilstand(TilGodkjenning)) {
                            throw BadRequestException("Behandlingen er ikke klar til å godkjennes")
                        }

                        val hendelse =
                            GodkjennBehandlingHendelse(
                                UUIDv7.ny(),
                                identForespørsel.ident,
                                call.behandlingId,
                                Saksbehandler(call.saksbehandlerId()),
                                LocalDateTime.now(),
                            )
                        hendelse.info("Godkjente behandling", identForespørsel.ident, call.saksbehandlerId(), AuditOperasjon.UPDATE)

                        hendelseMediator.behandle(hendelse, messageContext(identForespørsel.ident))

                        call.respond(HttpStatusCode.Created)
                    }

                    post("beslutt") {
                        val identForespørsel = call.receive<IdentForesporselDTO>()
                        val behandling = hentBehandling(personRepository, call.behandlingId)

                        // TODO: La dette egentlig komme fra modellen
                        if (!behandling.harTilstand(TilBeslutning)) {
                            // throw BadRequestException("Behandlingen er ikke til beslutning enda")
                        }

                        val hendelse =
                            BesluttBehandlingHendelse(
                                UUIDv7.ny(),
                                identForespørsel.ident,
                                call.behandlingId,
                                Saksbehandler(call.saksbehandlerId()),
                                LocalDateTime.now(),
                            )
                        hendelse.info("Besluttet behandling", identForespørsel.ident, call.saksbehandlerId(), AuditOperasjon.UPDATE)

                        hendelseMediator.behandle(hendelse, messageContext(identForespørsel.ident))

                        hendelse.harAktiviteter()

                        call.respond(HttpStatusCode.Created)
                    }
                    post("send-tilbake") {
                        val identForespørsel = call.receive<IdentForesporselDTO>()
                        val hendelse = SendTilbakeHendelse(UUIDv7.ny(), identForespørsel.ident, call.behandlingId, LocalDateTime.now())
                        hendelse.info(
                            "Sendte behandling tilbake til saksbehandler",
                            identForespørsel.ident,
                            call.saksbehandlerId(),
                            AuditOperasjon.UPDATE,
                        )

                        hendelseMediator.behandle(hendelse, messageContext(identForespørsel.ident))

                        call.respond(HttpStatusCode.Created)
                    }

                    post("avbryt") {
                        val identForespørsel = call.receive<IdentForesporselDTO>()

                        val hendelse =
                            AvbrytBehandlingHendelse(
                                UUIDv7.ny(),
                                identForespørsel.ident,
                                call.behandlingId,
                                "Avbrutt av saksbehandler",
                                LocalDateTime.now(),
                            )
                        hendelse.info("Avbrøt behandling", identForespørsel.ident, call.saksbehandlerId(), AuditOperasjon.UPDATE)

                        hendelseMediator.behandle(hendelse, messageContext(identForespørsel.ident))

                        call.respond(HttpStatusCode.Created)
                    }

                    put("opplysning/{opplysningId}") {
                        val behandlingId = call.behandlingId
                        val opplysningId = call.opplysningId
                        withLoggingContext(
                            "behandlingId" to behandlingId.toString(),
                        ) {
                            val oppdaterOpplysningRequestDTO = call.receive<OppdaterOpplysningRequestDTO>()
                            val behandling = hentBehandling(personRepository, behandlingId)

                            if (behandling.harTilstand(Redigert)) {
                                throw BadRequestException("Kan ikke redigere opplysninger før forrige redigering er ferdig")
                            }

                            val opplysning = behandling.opplysninger().finnOpplysning(opplysningId)
                            val svar =
                                OpplysningsSvar(
                                    behandlingId,
                                    opplysningId,
                                    opplysning.opplysningstype.id,
                                    behandling.behandler.ident,
                                    HttpVerdiMapper(oppdaterOpplysningRequestDTO).map(opplysning.opplysningstype.datatype),
                                    call.saksbehandlerId(),
                                )

                            messageContext(behandling.behandler.ident).publish(svar.toJson())
                            auditlogg.oppdater("Oppdaterte opplysning", behandling.behandler.ident, call.saksbehandlerId())

                            logger.info { "Venter på endring i behandling" }
                            ventPåBehandling(personRepository, behandlingId) {
                                sjekkAt("opplysningen fjernes") {
                                    runCatching { opplysninger().finnOpplysning(opplysningId) }.isFailure
                                }
                                sjekkAt("opplysningen er lagt til") {
                                    runCatching { opplysninger().finnOpplysning(opplysning.opplysningstype) }.isSuccess
                                }
                                sjekkAt("behandlingen er i riktig tilstand") {
                                    harTilstand(ForslagTilVedtak) || harTilstand(TilGodkjenning)
                                }
                            }
                            logger.info { "Svarer med at opplysning er oppdatert" }

                            call.respond(
                                HttpStatusCode.OK,
                                KvitteringDTO(
                                    behandlingId = behandlingId,
                                ),
                            )
                        }
                    }

                    get("avklaring") {
                        val behandlingId = call.behandlingId
                        val behandling = hentBehandling(personRepository, behandlingId)
                        call.respond(HttpStatusCode.OK, behandling.avklaringer().map { it.tilAvklaringDTO() })
                    }

                    put("avklaring/{avklaringId}") {
                        val behandlingId = call.behandlingId
                        val avklaringId = call.avklaringId
                        val kvitteringDTO = call.receive<KvitterAvklaringRequestDTO>()
                        val behandling = hentBehandling(personRepository, behandlingId)

                        val avklaring =
                            behandling.avklaringer().singleOrNull { it.id == avklaringId }
                                ?: throw NotFoundException("Avklaring ikke funnet")

                        if (!avklaring.kanKvitteres) {
                            call.respond(HttpStatusCode.BadRequest)
                            return@put
                        }

                        hendelseMediator.behandle(
                            AvklaringKvittertHendelse(
                                meldingsreferanseId = UUIDv7.ny(),
                                ident = behandling.behandler.ident,
                                avklaringId = avklaringId,
                                behandlingId = behandling.behandlingId,
                                saksbehandler = call.saksbehandlerId(),
                                begrunnelse = kvitteringDTO.begrunnelse,
                                opprettet = LocalDateTime.now(),
                            ),
                            messageContext(behandling.behandler.ident),
                        )

                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}

internal fun hentBehandling(
    personRepository: PersonRepository,
    behandlingId: UUID,
) = personRepository.hentBehandling(behandlingId) ?: throw NotFoundException("Behandling ikke funnet")

internal class ApiMessageContext(
    val rapid: MessageContext,
    val ident: String,
) : MessageContext {
    override fun publish(message: String) {
        publish(ident, message)
    }

    override fun publish(
        key: String,
        message: String,
    ) {
        rapid.publish(ident, message)
    }

    override fun rapidName() = "API"
}

private val ApplicationCall.opplysningId: UUID
    get() {
        val opplysningId = parameters["opplysningId"] ?: throw IllegalArgumentException("OpplysningId må være satt")
        return UUID.fromString(opplysningId)
    }
private val ApplicationCall.behandlingId: UUID
    get() {
        val behandlingId = parameters["behandlingId"] ?: throw IllegalArgumentException("BehandlingId må være satt")
        return UUID.fromString(behandlingId)
    }

private val ApplicationCall.avklaringId: UUID
    get() {
        val avklaringId = parameters["avklaringId"] ?: throw IllegalArgumentException("BehandlingId må være satt")
        return UUID.fromString(avklaringId)
    }

private val OtelTraceIdPlugin =
    createApplicationPlugin("OtelTraceIdPlugin") {
        onCallRespond { call, _ ->
            val traceId = runCatching { Span.current().spanContext.traceId }.getOrNull()
            traceId?.let { call.response.headers.append("X-Trace-Id", it) }
        }
    }

@Suppress("UNCHECKED_CAST")
private class HttpVerdiMapper(
    private val oppdaterOpplysningRequestDTO: OppdaterOpplysningRequestDTO,
) : VerdiMapper {
    override fun <T : Comparable<T>> map(datatype: Datatype<T>): T =
        when (datatype) {
            Heltall -> oppdaterOpplysningRequestDTO.verdi.toInt() as T
            Boolsk -> oppdaterOpplysningRequestDTO.verdi.toBoolean() as T
            Desimaltall -> oppdaterOpplysningRequestDTO.verdi.toDouble() as T
            Penger -> oppdaterOpplysningRequestDTO.verdi.toDouble() as T
            Dato -> oppdaterOpplysningRequestDTO.verdi.let { LocalDate.parse(it) } as T
            else -> throw BadRequestException("Datatype $datatype støttes ikke å redigere i APIet enda")
        }
}
