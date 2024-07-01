package no.nav.dagpenger.behandling.mediator.mottak

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.HendelseMessage
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class SøknadInnsendtMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                // TODO: Her bør vi lytte å hendelsen fra dp-soknad-orkestrator. Nå kan vi lage behov før dp-soknad-orkestrator har mottatt søknaden.
                validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
                validate { it.demandAny("type", listOf("NySøknad")) }
                validate { it.requireKey("fødselsnummer") }
                validate { it.requireKey("fagsakId") }
                validate { it.requireValue("bruk-dp-behandling", true) }
                validate {
                    it.require("søknadsData") { data ->
                        data["søknad_uuid"].asUUID()
                    }
                }
                validate { it.interestedIn("@id", "@opprettet") }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknadId = packet["søknadsData"]["søknad_uuid"].asUUID().toString()
        withLoggingContext("søknadId" to søknadId) {
            Span.current().apply {
                setAttribute("app.river", name())
                setAttribute("app.søknadId", søknadId)
            }
            logger.info { "Mottok søknad innsendt hendelse" }
            sikkerlogg.info { "Mottok søknad innsendt hendelse: ${packet.toJson()}" }
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
    override val ident get() = packet["fødselsnummer"].asText()
    private val hendelse
        get() =
            SøknadInnsendtHendelse(
                id,
                ident,
                søknadId = packet["søknadsData"]["søknad_uuid"].asUUID(),
                gjelderDato = java.time.LocalDate.now(),
                // TODO: Vi burde alltid ha fagsakId, og defaulte til 0 er ikke så lurt
                fagsakId = packet["fagsakId"].asInt(0),
            ).also {
                if (it.fagsakId == 0) logger.warn { "Søknad mottatt uten fagsakId" }
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
