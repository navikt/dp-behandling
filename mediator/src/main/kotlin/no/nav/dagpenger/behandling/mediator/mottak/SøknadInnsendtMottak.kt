package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.HendelseMessage
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse

internal class SøknadInnsendtMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "søknad_behandlingsklar") }
                validate {
                    it.requireKey(
                        "ident",
                        "innsendt",
                        "fagsakId",
                        "søknadId",
                    )
                }
                validate { it.interestedIn("@id", "@opprettet") }
                validate { it.interestedIn("journalpostId") }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknadId"].asUUID().toString()
        Span.current().apply {
            setAttribute("app.river", name())
            setAttribute("app.søknadId", søknadId)
        }
        withLoggingContext("søknadId" to søknadId) {
            logger.info { "Mottok behandlingsklar søknad" }
            sikkerlogg.info { "Mottok behandlingsklar søknad: ${packet.toJson()}" }
            val message = SøknadInnsendtMessage(packet)
            message.behandle(messageMediator, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadInnsendtMottak")
    }
}

internal class SøknadInnsendtMessage(
    private val packet: JsonMessage,
) : HendelseMessage(packet) {
    override val ident get() = packet["ident"].asText()
    private val søknadId = packet["søknadId"].asUUID()
    private val hendelse: SøknadInnsendtHendelse
        get() {
            return SøknadInnsendtHendelse(
                id,
                ident,
                søknadId = søknadId,
                gjelderDato = packet["innsendt"].asLocalDateTime().toLocalDate(),
                fagsakId = packet["fagsakId"].asInt(),
                opprettet,
            )
        }

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        withLoggingContext(hendelse.kontekstMap()) {
            logger.info { "Behandler søknad innsendt hendelse" }
            mediator.behandle(hendelse, this, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
