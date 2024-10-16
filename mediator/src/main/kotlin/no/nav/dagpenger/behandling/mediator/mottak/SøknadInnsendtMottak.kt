package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.konfigurasjon.støtterInnvilgelse
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import no.nav.dagpenger.behandling.mediator.MessageMediator
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.mediator.melding.HendelseMessage
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse

internal class SøknadInnsendtMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
                validate { it.demandAny("type", listOf("NySøknad")) }
                validate { it.requireKey("fødselsnummer") }
                validate { it.requireKey("fagsakId") }
                validate {
                    it.require("søknadsData") { data ->
                        data["søknad_uuid"].asUUID()
                    }
                }
                validate { it.interestedIn("@id", "@opprettet") }
                validate { it.interestedIn("journalpostId") }
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
    private val søknadId = packet["søknadsData"]["søknad_uuid"].asUUID()
    private val hendelse: SøknadInnsendtHendelse
        get() {
            return SøknadInnsendtHendelse(
                id,
                ident,
                søknadId = søknadId,
                gjelderDato = packet["@opprettet"].asLocalDateTime().toLocalDate(),
                // TODO: Vi burde alltid ha fagsakId, og defaulte til 0 er ikke så lurt
                fagsakId = packet["fagsakId"].asInt(0),
                opprettet,
                støtterInnvilgelse || kandidatplukk(),
            ).also {
                if (it.fagsakId == 0) logger.warn { "Søknad mottatt uten fagsakId" }
            }
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

    private val kandidater =
        setOf(
            678969908,
            678982085,
            678996119,
        )

    private fun kandidatplukk(): Boolean =
        (packet["journalpostId"].asInt() in kandidater).also {
            if (it) logger.info { "Fant en søknad som er kandidat. " }
        }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
