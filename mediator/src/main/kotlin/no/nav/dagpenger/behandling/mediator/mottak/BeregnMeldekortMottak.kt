package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.HendelseMessage
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.regel.BeregnMeldekortHendelse

internal class BeregnMeldekortMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
    private val meldekortRepository: MeldekortRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.requireValue("@event_name", "beregn_meldekort") }
                validate { it.requireKey("meldekortId", "ident") }
            }.register(this)
    }

    private val skipMeldekort =
        setOf<String>(
            "0195eb1b-0e02-7d0e-abda-97d2cf6b791d",
            "01956154-41c7-7c00-aa90-e1f1b8b9cd0f",
            "01957044-adad-7e3a-9ad2-41cc0b0b6266",
            "01957ede-3ff5-7e41-9b49-dcbdb8e4be8b",
            "01957fcf-ec3a-7c95-aa16-57f41bdad467",
            "0195801f-0a5c-7475-a181-13ee1ce3b4e6",
            "01958025-9040-71f1-a318-d244433592ae",
            "01958a64-b67d-759e-b7ce-ad7830f88998",
            "01958e85-75e9-7d8d-a798-f3fea02f4f57",
            "0195a3b9-4eab-7ef8-b27a-fe25221237e3",
            "0195a8c6-f175-7d9f-a869-d176daf98175",
        )

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val meldekortId = packet["meldekortId"].asUUID()
        if (meldekortId.toString() in skipMeldekort) {
            log.info { "Skipper $meldekortId" }
            return
        }

        withLoggingContext(
            "meldekortId" to meldekortId.toString(),
        ) {
            val meldekort =
                meldekortRepository.hent(meldekortId)
                    ?: throw IllegalStateException("Meldekort med id $meldekortId finnes ikke")
            val message = BeregnMeldekortMessage(packet, meldekort)
            message.behandle(messageMediator, context)
        }
    }

    private companion object {
        private val log = KotlinLogging.logger {}
    }

    class BeregnMeldekortMessage(
        packet: JsonMessage,
        private val meldekort: Meldekort,
    ) : HendelseMessage(packet) {
        val meldekortId = packet["meldekortId"].asUUID()
        override val ident = packet["ident"].asText()

        override fun behandle(
            mediator: IMessageMediator,
            context: MessageContext,
        ) {
            mediator.behandle(hendelse, this, context)
        }

        private val hendelse
            get() = BeregnMeldekortHendelse(id, ident, meldekortId, opprettet, meldekort)
    }
}
