package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.melding.KafkaMelding
import no.nav.dagpenger.behandling.modell.hendelser.ArbeidssøkerperiodeId
import no.nav.dagpenger.regel.hendelse.AvsluttetArbeidssøkerperiode
import no.nav.dagpenger.regel.hendelse.AvsluttetArbeidssøkerperiodeHendelse

internal class AvsluttetArbeidssøkerperiodeMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: IMessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "utmeldt_fra_arbeidssøkerregisteret") }
                validate { it.requireKey("ident", "periodeId", "fastsattMeldedato", "avregistrertTidspunkt", "årsak") }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logger.info { "Mottok utmeldt_fra_arbeidssøkerregisteret" }

        val message = AvsluttetArbeidssøkerperiodeMessage(packet)
        message.behandle(messageMediator, context)
    }

    class AvsluttetArbeidssøkerperiodeMessage(
        packet: JsonMessage,
    ) : KafkaMelding(packet) {
        private val periodeId = packet["periodeId"].asUUID()
        private val fastsattMeldingsdag = packet["fastsattMeldedato"].asLocalDate()
        private val avsluttetTidspunkt = packet["avregistrertTidspunkt"].asLocalDateTime()
        private val årsak = packet["årsak"].asText().let { Årsak.valueOf(it) }

        override val ident = packet["ident"].asText()

        private val hendelse =
            AvsluttetArbeidssøkerperiodeHendelse(
                meldingsreferanseId = id,
                ident = ident,
                opprettet = opprettet,
                avsluttetArbeidssøkerperiode =
                    AvsluttetArbeidssøkerperiode(
                        ArbeidssøkerperiodeId(periodeId),
                        fastsattMeldingsdag,
                        avsluttetTidspunkt,
                        opprettet,
                        sagtNei = årsak == Årsak.UTMELDT_PÅ_MELDEKORT,
                        fristBrutt = årsak == Årsak.MELDEPLIKT_BRUTT,
                        manueltAvregistrert = årsak == Årsak.UTMELDT_I_ASR,
                    ),
            )

        override fun behandle(
            mediator: IMessageMediator,
            context: MessageContext,
        ) {
            mediator.behandle(hendelse, this, context)
        }
    }

    private enum class Årsak {
        HAR_IKKE_RETT_PÅ_DAGPENGER,
        UTMELDT_I_ASR,
        MELDEPLIKT_BRUTT,
        UTMELDT_PÅ_MELDEKORT,
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
