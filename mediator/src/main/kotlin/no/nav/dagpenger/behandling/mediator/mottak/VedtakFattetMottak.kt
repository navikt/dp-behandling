package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.withLoggingContext
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId

class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val meldekortRepository: MeldekortRepository,
) : River.PacketListener {
    companion object {
        private val logger = mu.KotlinLogging.logger {}
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "vedtak_fattet")
                    it.requireValue("behandletHendelse.type", "Meldekort")
                }
                validate { it.requireKey("behandlingId", "behandletHendelse") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        withLoggingContext(
            "behandlingId" to packet["behandlingId"].asText(),
            "event_name" to "vedtak_fattet",
        ) {
            logger.info { "Mottok vedtak_fattet melding" }
            val meldekortId = packet["behandletHendelse"]["id"].asText()
            meldekortRepository.markerSomFerdig(MeldekortId(meldekortId))
        }
    }
}
