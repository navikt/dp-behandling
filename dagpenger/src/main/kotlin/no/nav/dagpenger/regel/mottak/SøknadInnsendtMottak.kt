package no.nav.dagpenger.regel.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.hendelse.Søknadstype
import no.nav.dagpenger.regelverk.HendelseMottaker
import no.nav.dagpenger.regelverk.asUUID
import no.nav.dagpenger.regelverk.melding.KafkaMelding

class SøknadInnsendtMottak(
    rapidsConnection: RapidsConnection,
    private val hendelseMottaker: HendelseMottaker,
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
                validate { it.interestedIn("journalpostId", "type") }
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
            withLoggingContext(message.hendelse.kontekstMap()) {
                logger.info { "Behandler søknad innsendt hendelse" }
                hendelseMottaker.behandle(message.hendelse, message, context)
            }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadInnsendtMottak")
    }
}

class SøknadInnsendtMessage(
    private val packet: JsonMessage,
) : KafkaMelding(packet) {
    override val ident get() = packet["ident"].asString()
    private val søknadId = packet["søknadId"].asUUID()
    private val søknadstype =
        packet["type"].let { node ->
            if (node.isMissingNode) Søknadstype.NySøknad else Søknadstype.valueOf(node.textValue())
        }

    internal val hendelse: SøknadInnsendtHendelse
        get() {
            return SøknadInnsendtHendelse(
                id,
                ident,
                søknadId = søknadId,
                gjelderDato = packet["innsendt"].asLocalDateTime().toLocalDate(),
                fagsakId = packet["fagsakId"].asInt(),
                opprettet,
                søknadstype,
            )
        }
}
