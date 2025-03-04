package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
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
import no.nav.dagpenger.behandling.mediator.melding.HendelseMessage
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortInnsendtHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.uuid.UUIDv7
import kotlin.time.Duration

internal class MeldekortInnsendtMottak(
    rapidsConnection: RapidsConnection,
    private val messageMediator: MessageMediator,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "meldekort_innsendt") }
                validate { it.requireKey("ident") }
                validate { it.requireKey("id") }
                validate { it.requireKey("periode", "kilde", "dager", "innsendtTidspunkt") }
                validate { it.interestedIn("korrigeringAv") }
            }.register(this)
    }

    @WithSpan
    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val meldekortId = packet["id"].asLong()
        Span.current().apply {
            setAttribute("app.river", name())
            setAttribute("app.meldekortId", meldekortId.toString())
        }

        withLoggingContext(
            "meldekortId" to meldekortId.toString(),
        ) {
            val message = MeldekortInnsendtMessage(packet)
            logger.info("Vi har mottatt et meldekort")
            message.behandle(messageMediator, context)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

internal class MeldekortInnsendtMessage(
    private val packet: JsonMessage,
) : HendelseMessage(packet) {
    override val ident get() = packet["ident"].asText()

    override fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    ) {
        withLoggingContext(hendelse.kontekstMap()) {
            logger.info { "Behandler meldekort: ${hendelse.meldekortId}" }
            mediator.behandle(hendelse, this, context)
        }
    }

    private val hendelse
        get() =
            MeldekortInnsendtHendelse(
                id = UUIDv7.ny(),
                meldingsreferanseId = packet["@id"].asUUID(),
                ident = packet["ident"].asText(),
                meldekortId = packet["id"].asLong(),
                innsendtTidspunkt = packet["innsendtTidspunkt"].asLocalDateTime(),
                fom = packet["periode"]["fraOgMed"].asLocalDate(),
                tom = packet["periode"]["tilOgMed"].asLocalDate(),
                kilde =
                    MeldekortKilde(
                        rolle = packet["kilde"]["rolle"].asText(),
                        ident = packet["kilde"]["ident"].asText(),
                    ),
                opprettet = packet["@opprettet"].asLocalDateTime(),
                korrigeringAv = packet["korrigeringAv"].takeIf { !it.isMissingOrNull() }?.asLong(),
                dager =
                    packet["dager"].map { dag ->
                        Dag(
                            dato = dag["dato"].asLocalDate(),
                            // todo: Vi må få dette feltet fra team ramp.
                            meldt = dag["meldt"]?.takeIf { !it.isMissingOrNull() }?.asBoolean() ?: true,
                            aktiviteter =
                                dag["aktiviteter"].map {
                                    MeldekortAktivitet(
                                        type =
                                            when (it["type"].asText()) {
                                                "Arbeid" -> AktivitetType.Arbeid
                                                "Syk" -> AktivitetType.Syk
                                                "Utdanning" -> AktivitetType.Utdanning
                                                "Fravaer" -> AktivitetType.Fravær
                                                else -> throw IllegalArgumentException("Ukjent aktivitetstype '${it["type"].asText()}'")
                                            },
                                        timer =
                                            if (it.has("timer")) {
                                                Duration.parseIsoString(it["timer"].asText())
                                            } else {
                                                null
                                            },
                                    )
                                },
                        )
                    },
            )

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
