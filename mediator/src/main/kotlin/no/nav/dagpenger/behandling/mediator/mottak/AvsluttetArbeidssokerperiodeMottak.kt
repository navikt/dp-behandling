package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.regel.hendelse.AvsluttetArbeidssokerperiodeHendelse

internal class AvsluttetArbeidssokerperiodeMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: IMessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "avsluttet_arbeidssokerperiode") }
                validate {
                    it.requireKey("ident", "avsluttetTidspunkt")
                    it.interestedIn("fastsattMeldingsdag")
                    it.interestedIn("@id", "@opprettet")
                }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        Span.current().setAttribute("app.river", name())
        logger.info { "Mottok avsluttet arbeidssøkerperiode" }
        val message = AvsluttetArbeidssokerperiodeMessage(packet)
        message.behandle(messageMediator, context)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

internal class AvsluttetArbeidssokerperiodeMessage(
    private val packet: JsonMessage,
) : KafkaMelding(packet) {
    override val ident: String = packet["ident"].asText()

    private val hendelse
        get() =
            AvsluttetArbeidssokerperiodeHendelse(
                meldingsreferanseId = id,
                ident = ident,
                avsluttetTidspunkt = packet["avsluttetTidspunkt"].asLocalDateTime(),
                fastsattMeldingsdag = packet["fastsattMeldingsdag"].asOptionalLocalDate(),
                opprettet = opprettet,
            )

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }
}
