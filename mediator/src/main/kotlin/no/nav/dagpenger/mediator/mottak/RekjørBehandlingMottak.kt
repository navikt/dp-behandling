package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.mediator.IMessageMediator
import no.nav.dagpenger.mediator.MessageMediator
import no.nav.dagpenger.mediator.asUUID
import no.nav.dagpenger.mediator.melding.HåndterbarKafkaMelding
import no.nav.dagpenger.modell.hendelser.RekjørBehandlingHendelse
import no.nav.dagpenger.opplysning.Opplysningstype

internal class RekjørBehandlingMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
    private val opplysningstyper: Set<Opplysningstype<*>>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "rekjør_behandling") }
                validate { it.requireKey("ident", "behandlingId") }
                validate { it.interestedIn("oppfriskOpplysningIder") }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()

        withLoggingContext("behandlingId" to behandlingId.toString()) {
            Span.current().apply {
                setAttribute("app.river", name())
                setAttribute("app.behandlingId", behandlingId.toString())
            }
            logger.info { "Mottok hendelse om at behandlingen skal rekjøres" }
            sikkerlogg.info { "Mottok hendelse om at behandlingen skal rekjøres: ${packet.toJson()}" }

            val message = RekjørBehandlingMessage(packet, opplysningstyper)
            message.behandle(messageMediator, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.RekjørBehandlingMottak")
    }
}

internal class RekjørBehandlingMessage(
    private val packet: JsonMessage,
    private val opplysningstyper: Set<Opplysningstype<*>>,
) : HåndterbarKafkaMelding(packet) {
    override val ident get() = packet["ident"].asString()

    private val oppfriskOpplysninger: List<Opplysningstype<*>> =
        packet["oppfriskOpplysningIder"].values().map {
            val opplysningstypeId = it.asUUID()
            opplysningstyper.single { opplysningstype -> opplysningstype.id.uuid == opplysningstypeId }
        }

    private val hendelse
        get() =
            RekjørBehandlingHendelse(
                id,
                ident,
                packet["behandlingId"].asUUID(),
                opprettet,
                oppfriskOpplysninger,
            )

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        withLoggingContext(hendelse.kontekstMap()) {
            logger.info { "Behandler RekjørBehandlingHendelse" }
            mediator.behandle(hendelse, this, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
