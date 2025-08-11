package no.nav.dagpenger.behandling.mediator.api

import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.acceptItems
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.trace.Span
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.aktivitetslogg.AuditOperasjon
import no.nav.dagpenger.behandling.api.models.AvklaringKvitteringDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.HendelseDTOTypeDTO
import no.nav.dagpenger.behandling.api.models.IdentForesporselDTO
import no.nav.dagpenger.behandling.api.models.KvitteringDTO
import no.nav.dagpenger.behandling.api.models.NyBehandlingDTO
import no.nav.dagpenger.behandling.api.models.NyOpplysningDTO
import no.nav.dagpenger.behandling.api.models.OppdaterOpplysningDTO
import no.nav.dagpenger.behandling.api.models.OpplysningstypeDTO
import no.nav.dagpenger.behandling.api.models.RekjoringDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerbegrunnelseDTO
import no.nav.dagpenger.behandling.mediator.IHendelseMediator
import no.nav.dagpenger.behandling.mediator.OpplysningSvarBygger.VerdiMapper
import no.nav.dagpenger.behandling.mediator.api.auth.saksbehandlerId
import no.nav.dagpenger.behandling.mediator.api.auth.saksbehandlerIdOrNull
import no.nav.dagpenger.behandling.mediator.api.melding.FjernOpplysning
import no.nav.dagpenger.behandling.mediator.api.melding.OpplysningsSvar
import no.nav.dagpenger.behandling.mediator.audit.Auditlogg
import no.nav.dagpenger.behandling.mediator.barnMapper
import no.nav.dagpenger.behandling.mediator.lagVedtakDTO
import no.nav.dagpenger.behandling.mediator.repository.ApiMelding
import no.nav.dagpenger.behandling.mediator.repository.ApiRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.Redigert
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.TilBeslutning
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.TilGodkjenning
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.AvklaringKvittertHendelse
import no.nav.dagpenger.behandling.modell.hendelser.BesluttBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.GodkjennBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SendTilbakeHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.BarnDatatype
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Datatype
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.InntektDataType
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Penger
import no.nav.dagpenger.opplysning.PeriodeDataType
import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Tekst
import no.nav.dagpenger.opplysning.ULID
import no.nav.dagpenger.regel.hendelse.OpprettBehandlingHendelse
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.collections.first

private val logger = KotlinLogging.logger { }

internal fun Application.behandlingApi(
    personRepository: PersonRepository,
    hendelseMediator: IHendelseMediator,
    auditlogg: Auditlogg,
    opplysningstyper: Set<Opplysningstype<*>>,
    apiRepositoryPostgres: ApiRepositoryPostgres,
    meterRegistry: PrometheusMeterRegistry? = null,
    messageContext: (ident: String) -> MessageContext,
) {
    authenticationConfig()
    install(OtelTraceIdPlugin)

    routing {
        swaggerUI(path = "openapi", swaggerFile = "behandling-api.yaml")

        get("/internal/prometrics") {
            if (meterRegistry == null) call.respond("")

            call.request.acceptItems().firstOrNull()?.let {
                val contentType = ContentType.parse(it.value)
                val metrics = meterRegistry!!.scrape(it.value)

                call.respondText(metrics, contentType)
            } ?: call.respond(HttpStatusCode.NotAcceptable, "Supported types: application/openmetrics-text and text/plain")
        }

        get("/") { call.respond(HttpStatusCode.OK) }
        get("/features") {
            call.respond(
                HttpStatusCode.OK,
                emptyMap<String, Boolean>(),
            )
        }
        get("/opplysningstyper") {
            val typer =
                opplysningstyper.map {
                    OpplysningstypeDTO(
                        opplysningTypeId = it.id.uuid,
                        behovId = it.behovId,
                        navn = it.navn,
                        datatype =
                            when (it.datatype) {
                                Boolsk -> DataTypeDTO.BOOLSK
                                Dato -> DataTypeDTO.DATO
                                Desimaltall -> DataTypeDTO.DESIMALTALL
                                Heltall -> DataTypeDTO.HELTALL
                                ULID -> DataTypeDTO.ULID
                                Penger -> DataTypeDTO.PENGER
                                InntektDataType -> DataTypeDTO.INNTEKT
                                BarnDatatype -> DataTypeDTO.BARN
                                Tekst -> DataTypeDTO.TEKST
                                PeriodeDataType -> DataTypeDTO.PERIODE
                            },
                    )
                }
            call.respond(HttpStatusCode.OK, typer)
        }

        authenticate("azureAd") {
            route("person/behandling") {
                post {
                    val nyBehandlingDto = call.receive<NyBehandlingDTO>()
                    val ident = nyBehandlingDto.ident
                    val person = personRepository.hent(ident.tilPersonIdentfikator()) ?: throw NotFoundException("Person ikke funnet")

                    val hendelseId =
                        when (nyBehandlingDto.hendelse?.type) {
                            HendelseDTOTypeDTO.SØKNAD -> SøknadId(UUID.fromString(nyBehandlingDto.hendelse!!.id))
                            HendelseDTOTypeDTO.MELDEKORT -> MeldekortId(nyBehandlingDto.hendelse!!.id)
                            HendelseDTOTypeDTO.MANUELL,
                            null,
                            -> ManuellId(UUIDv7.ny())
                        }

                    val melding = ApiMelding(nyBehandlingDto.ident)
                    val hendelse =
                        OpprettBehandlingHendelse(
                            meldingsreferanseId = melding.id,
                            ident = nyBehandlingDto.ident,
                            eksternId = hendelseId,
                            gjelderDato = nyBehandlingDto.prøvingsdato ?: LocalDate.now(),
                            begrunnelse = nyBehandlingDto.begrunnelse,
                            opprettet = LocalDateTime.now(),
                        )
                    apiRepositoryPostgres.behandle(melding) {
                        hendelse.info("Oppretter behandling manuelt", nyBehandlingDto.ident, call.saksbehandlerId(), AuditOperasjon.CREATE)
                        hendelseMediator.behandle(hendelse, messageContext(nyBehandlingDto.ident))
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        personRepository
                            .hent(ident.tilPersonIdentfikator())!!
                            .behandlinger()
                            .single { it.behandler.eksternId == hendelse.eksternId }
                            .tilBehandlingDTO(),
                    )
                }
            }

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

                        call.saksbehandlerIdOrNull()?.let {
                            auditlogg.les("Så en behandling", behandling.behandler.ident, it)
                        }

                        val vedtakOpplysninger = behandling.vedtakopplysninger

                        call.respond(
                            HttpStatusCode.OK,
                            vedtakOpplysninger.lagVedtakDTO(
                                behandling.behandler.ident.tilPersonIdentfikator(),
                            ),
                        )
                    }

                    get("klumpen") {
                        val behandling = hentBehandling(personRepository, call.behandlingId)

                        call.saksbehandlerIdOrNull()?.let {
                            auditlogg.les("Så en behandling", behandling.behandler.ident, it)
                        }

                        val vedtakOpplysninger = behandling.vedtakopplysninger

                        call.respond(
                            HttpStatusCode.OK,
                            vedtakOpplysninger.tilKlumpDTO(behandling.behandler.ident),
                        )
                    }

                    get("vurderinger") {
                        val behandling = hentBehandling(personRepository, call.behandlingId)

                        auditlogg.les("Så en behandling", behandling.behandler.ident, call.saksbehandlerId())

                        call.respond(HttpStatusCode.OK, behandling.tilSaksbehandlersVurderinger())
                    }

                    put("vurderinger/{opplysningId}") {
                        val begrunnelse = call.receive<SaksbehandlerbegrunnelseDTO>()

                        personRepository.lagreBegrunnelse(call.opplysningId, begrunnelse.begrunnelse)

                        call.respond(HttpStatusCode.Accepted)
                    }

                    post("godkjenn") {
                        val identForespørsel = call.receive<IdentForesporselDTO>()
                        val behandling = hentBehandling(personRepository, call.behandlingId)

                        // TODO: La dette egentlig komme fra modellen
                        if (!behandling.harTilstand(TilGodkjenning)) {
                            throw BadRequestException(
                                "Behandlingen er ikke klar til å godkjennes. Er i status ${behandling.tilstand().first}",
                            )
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

                    post("rekjor") {
                        val rekjøring = call.receive<RekjoringDTO>()

                        logger.info { "Kjører behandling på nytt, oppfrisker=${rekjøring.opplysninger}" }

                        val hendelse =
                            RekjørBehandlingHendelse(
                                UUIDv7.ny(),
                                rekjøring.ident,
                                call.behandlingId,
                                LocalDateTime.now(),
                                rekjøring.opplysninger ?: emptyList(),
                            ).apply {
                                info("Rekjør behandling", rekjøring.ident, call.saksbehandlerId(), AuditOperasjon.UPDATE)
                            }

                        when (rekjøring.opplysninger?.isEmpty()) {
                            // Hvis ingen opplysninger er endret kan vi kjøre synkront
                            null, true -> hendelseMediator.behandle(hendelse, messageContext(rekjøring.ident))
                            // Hvis opplysninger er endret må vi vente på at behovene løses
                            false -> {
                                val behandling = hentBehandling(personRepository, call.behandlingId)
                                // TODO: Vi bør klare å vente på alle endringene
                                val opplysning = behandling.opplysninger().finnOpplysning(rekjøring.opplysninger!!.first())

                                apiRepositoryPostgres.endreOpplysning(call.behandlingId, opplysning.opplysningstype.behovId) {
                                    hendelseMediator.behandle(hendelse, messageContext(rekjøring.ident))
                                }
                            }
                        }

                        call.respond(HttpStatusCode.Created)
                    }

                    put("opplysning/{opplysningId}") {
                        val behandlingId = call.behandlingId
                        val opplysningId = call.opplysningId
                        withLoggingContext(
                            "behandlingId" to behandlingId.toString(),
                        ) {
                            val oppdaterOpplysningRequestDTO = call.receive<OppdaterOpplysningDTO>()
                            val behandling = hentBehandling(personRepository, behandlingId)

                            if (behandling.harTilstand(Redigert)) {
                                throw BadRequestException("Kan ikke redigere opplysninger før forrige redigering er ferdig")
                            }

                            val opplysning = behandling.opplysninger().finnOpplysning(opplysningId)
                            if (!redigerbareOpplysninger.kanRedigere(opplysning.opplysningstype)) {
                                throw BadRequestException("Opplysningstype ${opplysning.opplysningstype} kan ikke redigeres")
                            }

                            logger.info {
                                """
                                Mottok en endring i behandlingId=$behandlingId, 
                                behovId=${opplysning.opplysningstype.behovId},
                                datatype=${opplysning.opplysningstype.datatype},
                                gyldigFraOgMed=${oppdaterOpplysningRequestDTO.gyldigFraOgMed},
                                gyldigTilOgMed=${oppdaterOpplysningRequestDTO.gyldigTilOgMed}
                                """.trimIndent()
                            }

                            val svar =
                                OpplysningsSvar(
                                    behandlingId,
                                    opplysning.opplysningstype.behovId,
                                    behandling.behandler.ident,
                                    HttpVerdiMapper(oppdaterOpplysningRequestDTO).map(opplysning.opplysningstype.datatype),
                                    call.saksbehandlerId(),
                                    oppdaterOpplysningRequestDTO.begrunnelse,
                                    oppdaterOpplysningRequestDTO.gyldigFraOgMed,
                                    oppdaterOpplysningRequestDTO.gyldigTilOgMed,
                                )

                            apiRepositoryPostgres.endreOpplysning(behandlingId, opplysning.opplysningstype.behovId) {
                                logger.info { "Starter en endring i behandling" }
                                messageContext(behandling.behandler.ident).publish(svar.toJson())
                                auditlogg.oppdater("Oppdaterte opplysning", behandling.behandler.ident, call.saksbehandlerId())
                                logger.info { "Venter på endring i behandling" }
                            }

                            logger.info { "Svarer med at opplysning er oppdatert" }

                            call.respond(HttpStatusCode.OK, KvitteringDTO(behandlingId))
                        }
                    }

                    delete("opplysning/{opplysningId}") {
                        val behandlingId = call.behandlingId
                        val opplysningId = call.opplysningId
                        withLoggingContext(
                            "behandlingId" to behandlingId.toString(),
                        ) {
                            val behandling = hentBehandling(personRepository, behandlingId)

                            if (behandling.harTilstand(Redigert)) {
                                throw BadRequestException("Kan ikke fjerne opplysning før forrige redigering er ferdig")
                            }

                            val opplysning = behandling.opplysninger().finnOpplysning(opplysningId)
                            if (!redigerbareOpplysninger.kanRedigere(opplysning.opplysningstype)) {
                                throw BadRequestException("Opplysningstype ${opplysning.opplysningstype} kan ikke redigeres")
                            }

                            val perioder = behandling.opplysninger().kunEgne.finnAlle(opplysning.opplysningstype)
                            if (perioder.size > 1 && perioder.first().id == opplysningId) {
                                throw BadRequestException("Kan ikke fjerne denne opplysningen, de påfølgende periodene må fjernes først")
                            }

                            logger.info {
                                """
                                Skal fjerne opplysning i behandlingId=$behandlingId, 
                                behovId=${opplysning.opplysningstype.behovId},
                                datatype=${opplysning.opplysningstype.datatype},
                                """.trimIndent()
                            }

                            val svar =
                                FjernOpplysning(
                                    behandlingId = behandlingId,
                                    opplysningId = opplysningId,
                                    behovId = opplysning.opplysningstype.behovId,
                                    ident = behandling.behandler.ident,
                                    saksbehandler = call.saksbehandlerId(),
                                )

                            apiRepositoryPostgres.endreOpplysning(behandlingId, opplysning.opplysningstype.behovId) {
                                logger.info { "Starter en fjerning av opplysning i behandling" }
                                messageContext(behandling.behandler.ident).publish(svar.toJson())
                                auditlogg.oppdater("Fjernet opplysning", behandling.behandler.ident, call.saksbehandlerId())
                                logger.info { "Venter på slettingen blir ferdig i behandling" }
                            }

                            logger.info { "Svarer med at opplysning er fjernet" }

                            call.respond(HttpStatusCode.OK, KvitteringDTO(behandlingId))
                        }
                    }

                    post("opplysning/") {
                        val behandlingId = call.behandlingId

                        withLoggingContext(
                            "behandlingId" to behandlingId.toString(),
                        ) {
                            val nyOpplysningDTO = call.receive<NyOpplysningDTO>()
                            val behandling = hentBehandling(personRepository, behandlingId)

                            if (behandling.harTilstand(Redigert)) {
                                throw BadRequestException("Kan ikke redigere opplysninger før forrige redigering er ferdig")
                            }

                            val opplysningstype =
                                opplysningstyper.singleOrNull { it.id.uuid == nyOpplysningDTO.opplysningstype }
                                    ?: throw NotFoundException("Opplysningstype med id ${nyOpplysningDTO.opplysningstype} ikke funnet")

                            if (!redigerbareOpplysninger.kanRedigere(opplysningstype)) {
                                throw BadRequestException("Opplysningstype $opplysningstype kan ikke redigeres")
                            }

                            logger.info {
                                """
                                Mottok en endring i behandlingId=$behandlingId, 
                                behovId=${opplysningstype.behovId},
                                datatype=${opplysningstype.datatype},
                                gyldigFraOgMed=${nyOpplysningDTO.gyldigFraOgMed},
                                gyldigTilOgMed=${nyOpplysningDTO.gyldigTilOgMed}
                                """.trimIndent()
                            }

                            val svar =
                                OpplysningsSvar(
                                    behandlingId,
                                    opplysningstype.behovId,
                                    behandling.behandler.ident,
                                    HttpVerdiMapper2(nyOpplysningDTO).map(opplysningstype.datatype),
                                    call.saksbehandlerId(),
                                    nyOpplysningDTO.begrunnelse,
                                    nyOpplysningDTO.gyldigFraOgMed,
                                    nyOpplysningDTO.gyldigTilOgMed,
                                )

                            apiRepositoryPostgres.endreOpplysning(behandlingId, opplysningstype.behovId) {
                                logger.info { "Starter en endring i behandling" }
                                messageContext(behandling.behandler.ident).publish(svar.toJson())
                                auditlogg.oppdater("Oppdaterte opplysning", behandling.behandler.ident, call.saksbehandlerId())
                                logger.info { "Venter på endring i behandling" }
                            }

                            logger.info { "Svarer med at opplysning er oppdatert" }

                            call.respond(HttpStatusCode.OK, KvitteringDTO(behandlingId))
                        }
                    }

                    get("avklaring") {
                        val behandlingId = call.behandlingId
                        val behandling = hentBehandling(personRepository, behandlingId)
                        call.respond(HttpStatusCode.OK, behandling.avklaringer().map { it.tilAvklaringDTO() })
                    }

                    put("avklaring/{avklaringId}") {
                        val behandlingId = call.behandlingId
                        withLoggingContext(
                            "behandlingId" to behandlingId.toString(),
                        ) {
                            val avklaringId = call.avklaringId
                            val kvitteringDTO = call.receive<AvklaringKvitteringDTO>()
                            val behandling = hentBehandling(personRepository, behandlingId)

                            require(!behandling.harTilstand(Redigert)) { "Kan ikke avklare om behandling står i tilstanden Redigert" }

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

    override fun publish(messages: List<OutgoingMessage>) = rapid.publish(messages)

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
// Deprecated, fjernes når frontend bruker opplysnignstype ID til å redigere opplysning
private class HttpVerdiMapper(
    private val oppdaterOpplysningRequestDTO: OppdaterOpplysningDTO,
) : VerdiMapper {
    override fun <T : Comparable<T>> map(datatype: Datatype<T>): T =
        when (datatype) {
            Heltall -> oppdaterOpplysningRequestDTO.verdi.toInt() as T
            Boolsk -> oppdaterOpplysningRequestDTO.verdi.toBoolean() as T
            Desimaltall -> oppdaterOpplysningRequestDTO.verdi.toDouble() as T
            Penger -> oppdaterOpplysningRequestDTO.verdi.toDouble() as T
            Dato -> oppdaterOpplysningRequestDTO.verdi.let { LocalDate.parse(it) } as T
            BarnDatatype -> barnMapper(oppdaterOpplysningRequestDTO.verdi) as T
            else -> throw BadRequestException("Datatype $datatype støttes ikke å redigere i APIet enda")
        }
}

@Suppress("UNCHECKED_CAST")
private class HttpVerdiMapper2(
    private val nyOpplysning: NyOpplysningDTO,
) : VerdiMapper {
    override fun <T : Comparable<T>> map(datatype: Datatype<T>): T =
        when (datatype) {
            Heltall -> nyOpplysning.verdi.toInt() as T
            Boolsk -> nyOpplysning.verdi.toBoolean() as T
            Desimaltall -> nyOpplysning.verdi.toDouble() as T
            Penger -> nyOpplysning.verdi.toDouble() as T
            Dato -> nyOpplysning.verdi.let { LocalDate.parse(it) } as T
            BarnDatatype -> barnMapper(nyOpplysning.verdi) as T
            else -> throw BadRequestException("Datatype $datatype støttes ikke å redigere i APIet enda")
        }
}
