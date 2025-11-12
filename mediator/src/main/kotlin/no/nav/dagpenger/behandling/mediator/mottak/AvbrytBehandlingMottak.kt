package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.modell.hendelser.AvbrytBehandlingHendelse
import java.util.UUID

internal class AvbrytBehandlingMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "avbryt_behandling") }
                validate { it.requireKey("ident") }
                validate { it.requireKey("behandlingId") }
                validate { it.interestedIn("@id", "@opprettet", "årsak") }
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
        Span.current().apply {
            setAttribute("app.river", name())
            setAttribute("app.behandlingId", behandlingId.toString())
        }
        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
        ) {
            if (behandlingId == UUID.fromString("019938f9-23cf-7abe-b3de-797c59c5df0f")) {
                logger.info { "Skipper 'avbryt_behandling' for behandling" }
            }
            logger.info { "Avbryter behandlingen" }
            val message = AvbrytBehandlingMessage(packet)
            message.behandle(messageMediator, context)
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logger.error { problems }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

internal class AvbrytBehandlingMessage(
    packet: JsonMessage,
) : KafkaMelding(packet) {
    private val årsak = packet["årsak"].asText("Avbrutt av datamaskinen")

    private val hendelse
        get() = AvbrytBehandlingHendelse(id, ident, behandlingId, årsak, opprettet)
    override val ident = packet["ident"].asText()

    private val behandlingId = packet["behandlingId"].asUUID()

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }
}
