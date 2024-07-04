package no.nav.dagpenger.behandling.mediator.mottak

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.HendelseMessage
import no.nav.dagpenger.behandling.modell.hendelser.PåminnelseHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class PåminnelseMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "behandling_står_fast") }
                validate { it.requireKey("ident", "behandlingId") }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()

        withLoggingContext("behandlingId" to behandlingId.toString()) {
            Span.current().apply {
                setAttribute("app.river", name())
                setAttribute("app.behandlingId", behandlingId.toString())
            }
            logger.info { "Mottok hendelse om at behandlingen står fast" }

            val message = BehandlingStårFastMessage(packet)
            message.behandle(messageMediator, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadInnsendtMottak")
    }
}

internal class BehandlingStårFastMessage(
    private val packet: JsonMessage,
) : HendelseMessage(packet) {
    override val ident get() = packet["ident"].asText()

    private val hendelse
        get() =
            PåminnelseHendelse(
                id,
                ident,
                packet["behandlingId"].asUUID(),
            )

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        withLoggingContext(hendelse.kontekstMap()) {
            logger.info { "Behandler PåminnelseHendelse" }
            mediator.behandle(hendelse, this, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
