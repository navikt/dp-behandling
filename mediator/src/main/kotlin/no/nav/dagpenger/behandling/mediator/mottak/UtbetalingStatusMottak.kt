package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.modell.hendelser.UtbetalingStatus

internal class UtbetalingStatusMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: IMessageMediator,
) : River.PacketListener {
    private val utbetalingstatus =
        listOf(
            "utbetaling_mottatt",
            "utbetaling_sendt",
            "utbetaling_feilet",
            "utbetaling_utført",
        )

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireAny(
                        "@event_name",
                        utbetalingstatus,
                    )
                }
                validate { packet ->
                    packet.requireKey("ident", "behandlingId", "sakId", "behandletHendelseId", "status")
                    packet.interestedIn("@id", "@opprettet")
                }
            }.register(this)
    }

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
            val message = UtbetalingStatusMessage(packet)
            logger.info { "Mottok hendelse utbetalingstatus for behandlingen - status ${packet["status"].asText()} " }

            message.behandle(messageMediator, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

internal class UtbetalingStatusMessage(
    private val packet: JsonMessage,
) : KafkaMelding(packet) {
    private val hendelse
        get() =
            UtbetalingStatus(
                id,
                ident,
                status =
                    when (packet["@event_name"].asText()) {
                        "utbetaling_mottatt" -> UtbetalingStatus.Status.MOTTATT
                        "utbetaling_sendt" -> UtbetalingStatus.Status.SENDT
                        "utbetaling_feilet" -> UtbetalingStatus.Status.FEILET
                        "utbetaling_utført" -> UtbetalingStatus.Status.UTFØRT
                        else -> throw IllegalArgumentException("Ukjent utbetalingshendelse ${packet["@event_name"].asText()}")
                    },
                behandlingId = behandlingId,
                opprettet = opprettet,
                behandletHendelseId = packet["behandletHendelseId"].asText(),
            )

    override val ident = packet["ident"].asText()

    private val behandlingId = packet["behandlingId"].asUUID()

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }
}
