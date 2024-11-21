package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.withLoggingContext
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.HendelseMessage
import no.nav.dagpenger.behandling.modell.hendelser.LåsOppHendelse

internal class OppgaveReturnertTilSaksbehandler(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "oppgave_returnert_til_saksbehandling") }
                validate { it.requireKey("ident") }
                validate { it.requireKey("behandlingId") }
                validate { it.interestedIn("@id", "@opprettet") }
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
            val message = ReturnerTilSaksbehandlerMessage(packet)
            message.behandle(messageMediator, context)
        }
    }
}

internal class ReturnerTilSaksbehandlerMessage(
    packet: JsonMessage,
) : HendelseMessage(packet) {
    private val hendelse
        get() = LåsOppHendelse(id, ident, behandlingId, opprettet)
    override val ident = packet["ident"].asText()

    private val behandlingId = packet["behandlingId"].asUUID()

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }
}
