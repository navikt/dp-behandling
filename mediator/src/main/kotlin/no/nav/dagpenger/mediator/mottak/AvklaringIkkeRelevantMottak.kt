package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mediator.IMessageMediator
import no.nav.dagpenger.mediator.MessageMediator
import no.nav.dagpenger.mediator.melding.HåndterbarKafkaMelding
import no.nav.dagpenger.modell.hendelser.AvklaringIkkeRelevantHendelse

internal class AvklaringIkkeRelevantMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "AvklaringIkkeRelevant") }
                validate { it.requireKey("ident") }
                validate { it.requireKey("avklaringId", "kode") }
                validate { it.requireKey("behandlingId") }
                validate { it.interestedIn("@id", "@opprettet", "@behovId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        withLoggingContext(
            "behovId" to packet["@behovId"].asString(),
            "behandlingId" to packet["behandlingId"].asString(),
            "avklaringId" to packet["avklaringId"].asString(),
        ) {
            logger.info { "Mottok at avklaring ikke er relevant for ${packet["kode"].asString()}" }
            val message = AvklaringIkkeRelevantMessage(packet)
            message.behandle(messageMediator, context)
        }
    }
}

internal class AvklaringIkkeRelevantMessage(
    packet: JsonMessage,
) : HåndterbarKafkaMelding(packet) {
    override val ident = packet["ident"].asString()

    private val hendelse
        get() = AvklaringIkkeRelevantHendelse(id, ident, avklaringId, kode, behandlingId, opprettet)

    private val avklaringId = packet["avklaringId"].asUUID()
    private val behandlingId = packet["behandlingId"].asUUID()
    private val kode = packet["kode"].asString()

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }
}
