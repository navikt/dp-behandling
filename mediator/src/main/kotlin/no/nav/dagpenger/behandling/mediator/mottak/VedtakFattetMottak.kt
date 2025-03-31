package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository

class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val meldekortRepository: MeldekortRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition { it.requireValue("@event_name", "vedtak_fattet") }
            precondition {
                it.require("behandletHendelse") {
                    it["type"].asText() == "Meldekort"
                }
            }
        }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val meldekortId = packet["behandletHendelse"]["id"].asUUID()
        meldekortRepository.behandlet(meldekortId)
    }
}
