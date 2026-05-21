package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.modell.hendelser.SamordningId
import no.nav.dagpenger.regel.hendelse.OpprettBehandlingHendelse
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.melding.KafkaMelding
import no.nav.dagpenger.uuid.UUIDv7
import tools.jackson.databind.JsonNode

class SamordningHendelseMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMottaker: HendelseMottaker,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "annen_ytelse_endret") }
                validate {
                    it.requireKey("ident", "tema")
                    it.require("tidspunkt", JsonNode::asLocalDateTime)
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
        val message = SamordningHendelseMessage(packet)
        logger.info { "Mottok hendelse om mulig samordning med ${message.ytelse} fra ${message.fom}" }
        hendelseMottaker.behandle(message.hendelse, message, context)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logger.error { problems }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    class SamordningHendelseMessage(
        packet: JsonMessage,
    ) : KafkaMelding(packet) {
        override val ident: String = packet["ident"].asText()

        val ytelse = packet["tema"].asText()
        val fom = packet["tidspunkt"].asLocalDateTime()

        internal val hendelse =
            OpprettBehandlingHendelse(
                meldingsreferanseId = packet.id.toUUID(),
                ident = ident,
                eksternId = SamordningId(UUIDv7.ny()),
                gjelderDato = fom.toLocalDate(),
                begrunnelse = "Fanget opp mulig endring i samordning mot $ytelse",
                opprettet = opprettet,
                startNyKjede = false,
            )
    }
}
