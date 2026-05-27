package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidstimerId
import no.nav.dagpenger.regel.OpplysningsTyper.trekkVedForsenMeldingId
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.util.UUID

class MeldekortBehandlingsresultatSignalMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behandlingsresultat")
                    it.requireValue("behandletHendelse.type", "Meldekort")
                }
                validate { it.requireKey("ident", "behandlingId", "behandletHendelse", "opplysninger", "førteTil") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val signal = signal(packet)
        if (!signal.skalPubliseres) return

        withLoggingContext("behandlingId" to packet["behandlingId"].asString()) {
            context.publish(
                packet["ident"].asString(),
                JsonMessage
                    .newMessage(
                        "meldekort_behandlingsresultat_signal",
                        mapOf(
                            "ident" to packet["ident"].asString(),
                            "behandlingId" to packet["behandlingId"].asString(),
                            "behandletHendelseId" to packet["behandletHendelse"]["id"].asString(),
                            "trigger" to
                                mapOf(
                                    "trekkVedForsenMelding" to signal.harTrekkVedForsenMelding,
                                    "arbeidsdagUtenArbeid" to signal.harArbeidsdagMedFalse,
                                    "arbeidstimerIkkeNull" to signal.harArbeidstimerIkkeNull,
                                    "avgjorelseStans" to signal.harStans,
                                    "nyOpplysningUtenforBeregning" to signal.harNyOpplysningUtenforBeregning,
                                ),
                        ),
                    ).toJson(),
            )
            logger.info { "Publiserte meldekort_behandlingsresultat_signal" }
        }
    }

    private fun signal(packet: JsonMessage): Signal {
        val nyePerioder =
            packet["opplysninger"].toList().flatMap { opplysning ->
                val opplysningTypeId = opplysning["opplysningTypeId"].asUUIDOrNull() ?: return@flatMap emptyList()
                opplysning["perioder"]
                    .toList()
                    .filter { it.erNyPeriode() }
                    .map { NyPeriode(opplysningTypeId, it) }
            }

        val harTrekkVedForsenMelding =
            nyePerioder.any {
                it.opplysningTypeId == trekkVedForsenMeldingId.uuid && it.periode.boolskVerdi() == true
            }

        val harArbeidsdagMedFalse =
            nyePerioder.any {
                it.opplysningTypeId == arbeidsdagId.uuid && it.periode.boolskVerdi() == false
            }

        val harArbeidstimerIkkeNull =
            nyePerioder.any {
                it.opplysningTypeId == arbeidstimerId.uuid &&
                    it.periode.desimaltallVerdi()?.compareTo(BigDecimal.ZERO) != 0
            }

        val harStans = packet["førteTil"].asString().equals("STANS", ignoreCase = true)
        val harNyOpplysningUtenforBeregning = nyePerioder.any { it.opplysningTypeId !in beregningOpplysningTypeIder }

        val harMeldekortSignal = harTrekkVedForsenMelding || harArbeidsdagMedFalse || harArbeidstimerIkkeNull
        val harStoppsignal = harStans || harNyOpplysningUtenforBeregning

        return Signal(
            harTrekkVedForsenMelding = harTrekkVedForsenMelding,
            harArbeidsdagMedFalse = harArbeidsdagMedFalse,
            harArbeidstimerIkkeNull = harArbeidstimerIkkeNull,
            harStans = harStans,
            harNyOpplysningUtenforBeregning = harNyOpplysningUtenforBeregning,
            skalPubliseres = harMeldekortSignal && harStoppsignal,
        )
    }

    private data class NyPeriode(
        val opplysningTypeId: UUID,
        val periode: JsonNode,
    )

    private data class Signal(
        val harTrekkVedForsenMelding: Boolean,
        val harArbeidsdagMedFalse: Boolean,
        val harArbeidstimerIkkeNull: Boolean,
        val harStans: Boolean,
        val harNyOpplysningUtenforBeregning: Boolean,
        val skalPubliseres: Boolean,
    )

    private fun JsonNode.erNyPeriode() = this["opprinnelse"].asString().equals("NY", ignoreCase = true)

    private fun JsonNode.boolskVerdi(): Boolean? {
        val verdiNode = this["verdi"]["verdi"]
        return when {
            verdiNode.isBoolean -> verdiNode.asBoolean()
            else -> verdiNode.asString().toBooleanStrictOrNull()
        }
    }

    private fun JsonNode.desimaltallVerdi(): BigDecimal? {
        val verdiNode = this["verdi"]["verdi"]
        return when {
            verdiNode.isNumber -> verdiNode.decimalValue()
            else -> verdiNode.asString().toBigDecimalOrNull()
        }
    }

    private fun JsonNode.asUUIDOrNull() = this.asString().let { runCatching { UUID.fromString(it) }.getOrNull() }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val beregningOpplysningTypeIder =
            Beregning.regelsett.produserer
                .map { it.id.uuid }
                .toSet()
    }
}
