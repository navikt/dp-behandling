package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId

class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val meldekortRepository: MeldekortRepository,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    // TODO: Lytte på behandlingsresultat i stedenfor vedtak_fattet
                    it.requireAny("@event_name", listOf("vedtak_fattet", "behandling_avbrutt"))
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
        ) {
            logger.info { "Markerer melderkortet som ferdig behandlet" }
            val meldekortId = packet["behandletHendelse"]["id"].asText()
            meldekortRepository.markerSomFerdig(MeldekortId(meldekortId))
        }
    }
}
