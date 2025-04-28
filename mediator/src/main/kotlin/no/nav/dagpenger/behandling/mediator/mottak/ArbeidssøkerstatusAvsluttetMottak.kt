package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.melding.HendelseMessage
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.periode
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.ArbeidssøkerstatusAvsluttet

internal class ArbeidssøkerstatusAvsluttetMottak(
    rapidsConnection: RapidsConnection,
    private val mediator: IMessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "arbeidssøkerstatus_endret") }
                validate { it.requireKey("ident", "fom", "tom", "avsluttetAv", "periodeId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val message = ArbeidssøkerstatusAvsluttetMessage(packet)
        message.behandle(mediator, context)
    }

    internal class ArbeidssøkerstatusAvsluttetMessage(
        private val packet: JsonMessage,
    ) : HendelseMessage(packet) {
        override val ident = packet["ident"].asText()

        override fun behandle(
            mediator: IMessageMediator,
            context: MessageContext,
        ) {
            mediator.behandle(hendelse, this, context)
        }

        private val fom get() = packet["fom"].asLocalDateTime().toLocalDate()
        private val tom get() = packet["tom"].asLocalDateTime().toLocalDate()
        private val periodeId get() = packet["periodeId"].asUUID()
        private val avsluttetAv get() = packet["avsluttetAv"].asText()

        private val hendelse
            get() =
                ArbeidssøkerstatusAvsluttet(
                    meldingsreferanseId = id,
                    ident = ident,
                    opprettet = opprettet,
                    periodeId = periodeId,
                    periode = Periode(fom, tom),
                    avsluttetAv = avsluttetAv,
                )
    }
}
