package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.regel.hendelse.BeregnFerietilleggHendelse
import java.util.UUID

internal class BeregnFerietilleggMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "beregn_ferietillegg") }
                validate { it.requireKey("opptjeningsår", "ident", "ferietilleggId") }
            }.register(this)
    }

    private val skipFerietillegg = setOf("")

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val ferietilleggId = packet["ferietilleggId"].asUUID()
        if (ferietilleggId.toString() in skipFerietillegg) {
            log.info { "Skipper $ferietilleggId" }
            return
        }

        withLoggingContext(
            "ferietilleggId" to ferietilleggId.toString(),
        ) {
            log.info { "Mottok beregn_ferietillegg" }
            val message = BeregnFerietilleggMessage(packet, ferietilleggId)
            message.behandle(messageMediator, context)
        }
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }

    class BeregnFerietilleggMessage(
        packet: JsonMessage,
        ferietilleggId: UUID,
    ) : KafkaMelding(packet) {
        val opptjeningsår = packet["opptjeningsår"].asInt()
        override val ident = packet["ident"].asText()

        override fun behandle(
            mediator: IMessageMediator,
            context: MessageContext,
        ) {
            mediator.behandle(hendelse, this, context)
        }

        private val hendelse = BeregnFerietilleggHendelse(id, ident, opprettet, opptjeningsår, ferietilleggId)
    }
}
