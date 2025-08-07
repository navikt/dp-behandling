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
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepository
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.regel.hendelse.BeregnMeldekortHendelse
import java.util.UUID

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

    private val skipMeldekort = setOf("01987e95-029f-7f78-a0f4-365441aeb8aa")

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
            log.info { "Mottok beregn_meldekort" }
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
    ) : KafkaMelding(packet) {
        val meldekortId: UUID = packet["meldekortId"].asUUID()
        override val ident = packet["ident"].asText()

        override fun behandle(
            mediator: IMessageMediator,
            context: MessageContext,
        ) {
            mediator.behandle(hendelse, this, context)
        }

        private val hendelse get() = BeregnMeldekortHendelse(id, ident, meldekortId, opprettet, meldekort)
    }
}
