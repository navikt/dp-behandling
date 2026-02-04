package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.modell.hendelser.OmgjøringId
import no.nav.dagpenger.regel.hendelse.OmgjøringHendelse
import no.nav.dagpenger.uuid.UUIDv7

internal class OmgjøringMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: IMessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "omgjør_behandling") }
                validate {
                    it.requireKey("ident")
                    it.requireKey("gjelderDato")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val message = OmgjøringMessage(packet)
        message.behandle(hendelseMediator, context)
    }
}

internal class OmgjøringMessage(
    packet: JsonMessage,
) : KafkaMelding(packet) {
    override val ident: String = packet["ident"].asText()
    private val hendelse =
        OmgjøringHendelse(
            meldingsreferanseId = id,
            ident = ident,
            eksternId = OmgjøringId(UUIDv7.ny()),
            gjelderDato = packet["gjelderDato"].asLocalDate(),
            opprettet = opprettet,
        )

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }
}
