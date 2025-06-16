package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asOptionalLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.regel.hendelse.OpprettBehandlingHendelse
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class OpprettBehandlingMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMediator: IMessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "opprett_behandling") }
                validate {
                    it.requireKey("ident")
                    it.interestedIn("prøvingsdato", "begrunnelse")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val message = OpprettBehandlingMessage(packet)
        message.behandle(hendelseMediator, context)
    }
}

internal class OpprettBehandlingMessage(
    packet: JsonMessage,
) : KafkaMelding(packet) {
    override val ident: String = packet["ident"].asText()
    private val hendelse =
        OpprettBehandlingHendelse(
            meldingsreferanseId = UUID.fromString(packet.id),
            ident = ident,
            eksternId = ManuellId(UUIDv7.ny()),
            gjelderDato = packet["prøvingsdato"].asOptionalLocalDate() ?: LocalDate.now(),
            begrunnelse = packet["begrunnelse"].asText(),
            opprettet = LocalDateTime.now(),
        )

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        mediator.behandle(hendelse, this, context)
    }
}
