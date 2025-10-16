package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.behandling.mediator.asUUID

internal class InnsendingFerdigstiltMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "innsending_ferdigstilt") }
                precondition { it.requireAny("type", listOf("NySøknad", "Gjenopptak")) }
                validate { it.requireKey("fødselsnummer") }
                validate { it.interestedIn("fagsakId") }
                validate {
                    it.require("søknadsData") { data ->
                        data["søknad_uuid"].asUUID()
                    }
                }
                validate { it.interestedIn("@id", "@opprettet") }
                validate { it.interestedIn("journalpostId") }
            }.register(this)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadInnsendtMottak")
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknadsData"]["søknad_uuid"].asUUID().toString()
        withLoggingContext("søknadId" to søknadId) {
            logger.info { "Mottok innsending ferdigstilt hendelse" }
            val message = InnsendingFerdigstiltMessage(packet)
            message.publish(context)
        }
    }
}

class InnsendingFerdigstiltMessage(
    packet: JsonMessage,
) {
    private val ident = packet["fødselsnummer"].asText()
    private val opprettet = packet["@opprettet"].asLocalDateTime()
    private val søknadId = packet["søknadsData"]["søknad_uuid"].asUUID()
    private val fagsakId =
        packet["fagsakId"].asInt(0).also {
            if (it == 0) logger.warn { "Søknad ($type) mottatt uten fagsakId" }
        }
    private val journalpostId = packet["journalpostId"].asInt()
    private val type = packet["type"].asText()

    private val melding =
        JsonMessage.newMessage(
            "søknad_behandlingsklar",
            mapOf(
                "ident" to ident,
                "søknadId" to søknadId,
                "fagsakId" to fagsakId,
                "innsendt" to opprettet,
                "journalpostId" to journalpostId,
                "type" to type,
            ),
        )

    fun publish(context: MessageContext) {
        context.publish(ident, melding.toJson())
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
