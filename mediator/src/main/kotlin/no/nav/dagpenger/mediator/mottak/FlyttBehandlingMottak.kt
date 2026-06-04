package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.mediator.IMessageMediator
import no.nav.dagpenger.mediator.melding.HåndterbarKafkaMelding
import no.nav.dagpenger.modell.hendelser.FlyttBehandlingHendelse

internal class FlyttBehandlingMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMottaker: IMessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "flytt_behandling") }
                validate {
                    it.requireKey(
                        "ident",
                        "behandlingId",
                        "nyBasertPåId",
                    )
                }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logger.info { "Mottok flytt_behandling" }

        val message = FlyttBehandlingMessage(packet)
        hendelseMottaker.behandle(message.hendelse, message, context)
    }

    internal class FlyttBehandlingMessage(
        packet: JsonMessage,
    ) : HåndterbarKafkaMelding(packet) {
        private val behandlingId = packet["behandlingId"].asUUID()
        private val nyBasertPåId = packet["nyBasertPåId"].asUUID()

        override val ident = packet["ident"].asString()

        internal val hendelse =
            FlyttBehandlingHendelse(
                meldingsreferanseId = id,
                ident = ident,
                behandlingId = behandlingId,
                nyBasertPåId = nyBasertPåId,
                opprettet = opprettet,
            )

        override fun behandle(
            mediator: IMessageMediator,
            context: MessageContext,
        ) {
            mediator.behandle(hendelse, this, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
