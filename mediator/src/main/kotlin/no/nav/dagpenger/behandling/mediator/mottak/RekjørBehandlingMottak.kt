package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.modell.hendelser.RekjørBehandlingHendelse

internal class RekjørBehandlingMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
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

            val message = RekjørBehandlingMessage(packet)
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
) : KafkaMelding(packet) {
    override val ident get() = packet["ident"].asText()

    private val hendelse
        get() =
            RekjørBehandlingHendelse(
                id,
                ident,
                packet["behandlingId"].asUUID(),
                opprettet,
                packet["oppfriskOpplysningIder"].map { it.asUUID() },
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
