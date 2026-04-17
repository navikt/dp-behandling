package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.behandling.mediator.jobber.GJustering

internal class GJusteringMottak(
    rapidsConnection: RapidsConnection,
    private val gJustering: GJustering,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "start_g_justering") }
                validate {
                    it.requireKey("fraOgMed", "tilOgMed")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fraOgMed = packet["fraOgMed"].asLocalDate()
        val tilOgMed = packet["tilOgMed"].asLocalDate()
        logger.info { "Mottok start_g_justering for perioden $fraOgMed–$tilOgMed" }
        gJustering.startGjustering(fraOgMed, tilOgMed, context)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
