package no.nav.dagpenger.behandling.mediator.api

import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.opentelemetry.api.trace.Span
import no.nav.dagpenger.aktivitetslogg.AuditOperasjon
import no.nav.dagpenger.behandling.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.api.models.DataTypeDTO
import no.nav.dagpenger.behandling.api.models.IdentForesporselDTO
import no.nav.dagpenger.behandling.api.models.OpplysningDTO
import no.nav.dagpenger.behandling.api.models.OpplysningskildeDTO
import no.nav.dagpenger.behandling.api.models.RegelDTO
import no.nav.dagpenger.behandling.api.models.UtledningDTO
import no.nav.dagpenger.behandling.mediator.PersonMediator
import no.nav.dagpenger.behandling.mediator.api.auth.saksbehandlerId
import no.nav.dagpenger.behandling.mediator.audit.Auditlogg
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.UUIDv7
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import no.nav.dagpenger.behandling.modell.hendelser.ForslagGodkjentHendelse
import no.nav.dagpenger.opplysning.Boolsk
import no.nav.dagpenger.opplysning.Dato
import no.nav.dagpenger.opplysning.Desimaltall
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.ULID
import org.apache.kafka.common.errors.ResourceNotFoundException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

@Resource("/behandling")
private class BehandlingRoute {
    @Resource("{behandlingId}")
    class Id(val behandlinger: BehandlingRoute = BehandlingRoute(), private val behandlingId: String) {
        val id: UUID get() = UUID.fromString(behandlingId)

        @Resource("/avbryt")
        class Avbryt(val behandling: Id)

        @Resource("/godkjenn")
        class Godkjenn(val behandling: Id)
    }
}

internal fun Application.behandlingApi(
    personRepository: PersonRepository,
    personMediator: PersonMediator,
    auditlogg: Auditlogg,
) {
    konfigurerApi()
    install(OtelTraceIdPlugin)

    routing {
        swaggerUI(path = "openapi", swaggerFile = "behandling-api.yaml")

        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("azureAd") {
            post<BehandlingRoute> {
                val identForespørsel = call.receive<IdentForesporselDTO>()
                val ident = identForespørsel.ident
                val person =
                    personRepository.hent(
                        ident.tilPersonIdentfikator(),
                    ) ?: throw ResourceNotFoundException("Person ikke funnet")

                auditlogg.les("Listet ut behandlinger", ident, call.saksbehandlerId())

                call.respond(HttpStatusCode.OK, person.behandlinger().map { it.tilBehandlingDTO() })
            }
            get<BehandlingRoute.Id> { request ->
                val behandling =
                    personRepository.hentBehandling(
                        request.id,
                    ) ?: throw ResourceNotFoundException("Behandling ikke funnet")

                auditlogg.les("Så en behandling", behandling.behandler.ident, call.saksbehandlerId())

                call.respond(HttpStatusCode.OK, behandling.tilBehandlingDTO())
            }
            post<BehandlingRoute.Id.Godkjenn> { request ->
                val identForespørsel = call.receive<IdentForesporselDTO>()
                // TODO: Her må vi virkelig finne ut hva vi skal gjøre. Dette er bare en placeholder
                val hendelse = ForslagGodkjentHendelse(UUIDv7.ny(), identForespørsel.ident, request.behandling.id)
                hendelse.varsel("Godkjente behandling", identForespørsel.ident, call.saksbehandlerId(), AuditOperasjon.UPDATE)
                personMediator.håndter(hendelse)
                call.respond(HttpStatusCode.Created)
            }
            post<BehandlingRoute.Id.Avbryt> { request ->
                val identForespørsel = call.receive<IdentForesporselDTO>()
                // TODO: Her må vi virkelig finne ut hva vi skal gjøre. Dette er bare en placeholder
                val hendelse = AvbrytBehandlingHendelse(UUIDv7.ny(), identForespørsel.ident, request.behandling.id)
                hendelse.varsel("Avbrøt behandling", identForespørsel.ident, call.saksbehandlerId(), AuditOperasjon.UPDATE)
                personMediator.håndter(hendelse)
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}

private val OtelTraceIdPlugin =
    createApplicationPlugin("OtelTraceIdPlugin") {
        onCallRespond { call, _ ->
            val traceId = runCatching { Span.current().spanContext.traceId }.getOrNull()
            traceId?.let { call.response.headers.append("X-Trace-Id", it) }
        }
    }

private fun Behandling.tilBehandlingDTO(): BehandlingDTO {
    return BehandlingDTO(
        behandlingId = this.behandlingId,
        tilstand = BehandlingDTO.Tilstand.valueOf(tilstand().first.name),
        opplysning =
            this.opplysninger().finnAlle().map { opplysning ->
                opplysning.tilOpplysningDTO()
            },
    )
}

private fun Opplysning<*>.tilOpplysningDTO(): OpplysningDTO {
    @Suppress("ktlint:standard:comment-wrapping")
    return OpplysningDTO(
        id = this.id,
        navn = this.opplysningstype.navn,
        verdi = this.verdi.toString(),
        status =
            when (this) {
                is Faktum -> OpplysningDTO.Status.Faktum
                is Hypotese -> OpplysningDTO.Status.Hypotese
            },
        gyldigFraOgMed = this.gyldighetsperiode.fom.tilApiDato(),
        gyldigTilOgMed = this.gyldighetsperiode.tom.tilApiDato(),
        datatype =
            when (this.opplysningstype.datatype) {
                Boolsk -> DataTypeDTO.boolsk
                Dato -> DataTypeDTO.dato
                Desimaltall -> DataTypeDTO.desimaltall
                Heltall -> DataTypeDTO.heltall
                ULID -> DataTypeDTO.ulid
            },
        kilde =
            this.kilde?.let {
                val registrert = it.registrert.tilOffsetTime()
                when (it) {
                    is Saksbehandlerkilde -> OpplysningskildeDTO("Saksbehandler", ident = it.ident, registrert = registrert)
                    is Systemkilde -> OpplysningskildeDTO("System", meldingId = it.meldingsreferanseId, registrert = registrert)
                }
            },
        utledetAv =
            this.utledetAv?.let { utledning ->
                UtledningDTO(
                    regel = RegelDTO(navn = utledning.regel),
                    opplysninger = utledning.opplysninger.map { it.tilOpplysningDTO() },
                )
            },
        redigerbar = this.kanRedigeres,
    )
}

private fun LocalDate.tilApiDato(): LocalDate? {
    return when (this) {
        LocalDate.MIN -> null
        LocalDate.MAX -> null
        else -> this
    }
}

private fun LocalDateTime.tilOffsetTime(): OffsetDateTime = this.atZone(ZoneId.systemDefault()).toOffsetDateTime()
