package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.modell.hendelser.FjernOpplysningHendelse
import no.nav.dagpenger.opplysning.OpplysningIkkeFunnetException
import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDateTime
import java.util.UUID

internal class FjernOpplysningMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: IMessageMediator,
    private val opplysningstyper: Set<Opplysningstype<*>>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAllOrAny("@behov", listOf("FjernOpplysning")) }
                precondition { it.requireValue("@final", true) }
                validate { it.requireKey("ident") }
                validate { it.requireKey("behandlingId") }
                validate { it.requireKey("opplysningId") }
                validate { it.requireKey("behovId") }
                validate { it.interestedIn("@id", "@opprettet", "@behovId") }
            }.register(this)
    }

    private val skipOpplysning = listOf("0197b113-1c65-7eaa-b4d1-b7d4dfee9313")

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behovId = packet["@behovId"].asText()
        val behandlingId = packet["behandlingId"].asUUID()
        val opplysningId = packet["opplysningId"].asUUID()

        if (skipOpplysning.contains(opplysningId.toString())) {
            logger.info { "Skipper opplysning med id=$opplysningId" }
            return
        }
        addOtelAttributes(behovId, behandlingId)

        withLoggingContext(
            "behovId" to behovId.toString(),
            "behandlingId" to behandlingId.toString(),
        ) {
            logger.info { "Mottok behov for å fjerne opplysning" }

            val opplysningstype = packet["behovId"].asText()
            val message = FjernOpplysningMessage(packet, opplysningstype)

            try {
                message.behandle(messageMediator, context)
            } catch (e: OpplysningIkkeFunnetException) {
                logger.warn(e) {
                    "Opplysning med id=$opplysningId ble ikke funnet og kan derfor ikke fjernes for behandlingId=$behandlingId"
                }
            }
        }
    }

    private fun addOtelAttributes(
        behovId: String,
        behandlingId: UUID,
    ) {
        Span.current().apply {
            setAttribute("app.river", name())
            setAttribute("app.behovId", behovId)
            setAttribute("app.behandlingId", behandlingId.toString())
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

internal class FjernOpplysningMessage(
    packet: JsonMessage,
    opplysningstype: String,
) : KafkaMelding(packet) {
    override val ident: String = packet["ident"].asText()
    private val hendelse =
        FjernOpplysningHendelse(
            meldingsreferanseId = UUID.fromString(packet.id),
            ident = ident,
            behandlingId = packet["behandlingId"].asUUID(),
            opplysningId = packet["opplysningId"].asUUID(),
            behovId = opplysningstype,
            opprettet = LocalDateTime.now(),
        )

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }
}
