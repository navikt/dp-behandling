package no.nav.dagpenger.ferietillegg.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.ferietillegg.hendelse.BeregnFerietilleggHendelse
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.asUUID
import no.nav.dagpenger.regelverk.melding.KafkaMelding
import java.util.UUID

class BeregnFerietilleggMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMottaker: HendelseMottaker,
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
            hendelseMottaker.behandle(message.hendelse, message, context)
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
        override val ident = packet["ident"].asString()

        internal val hendelse = BeregnFerietilleggHendelse(id, ident, opprettet, opptjeningsår, ferietilleggId)
    }
}
