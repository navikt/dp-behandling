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
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.modell.hendelser.ArbeidssøkerperiodeId
import no.nav.dagpenger.regel.hendelse.AvsluttetArbeidssøkerperiode
import no.nav.dagpenger.regel.hendelse.AvsluttetArbeidssøkerperiodeHendelse

internal class AvsluttetArbeidssøkerperiodeMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "avsluttet_arbeidssokerperiode") }
                validate { it.requireKey("ident", "avsluttetTidspunkt") }
                validate { it.interestedIn("fastsattMeldingsdag") }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logger.info { "Mottok avsluttet_arbeidssokerperiode" }

        val message = AvsluttetArbeidssøkerperiodeMessage(packet)
        message.behandle(messageMediator, context)
    }

    class AvsluttetArbeidssøkerperiodeMessage(
        packet: JsonMessage,
    ) : KafkaMelding(packet) {
        private val fastsattMeldingsdag = packet["fastsattMeldingsdag"].asOptionalLocalDate()
        private val avsluttetTidspunkt = packet["avsluttetTidspunkt"].asLocalDateTime()

        override val ident = packet["ident"].asText()

        private val hendelse =
            AvsluttetArbeidssøkerperiodeHendelse(
                meldingsreferanseId = id,
                ident = ident,
                opprettet = opprettet,
                avsluttetArbeidssøkerperiode =
                    AvsluttetArbeidssøkerperiode(
                        ArbeidssøkerperiodeId(id),
                        fastsattMeldingsdag,
                        avsluttetTidspunkt,
                        opprettet,
                    ),
            )

        override fun behandle(
            mediator: IMessageMediator,
            context: MessageContext,
        ) {
            mediator.behandle(hendelse, this, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
