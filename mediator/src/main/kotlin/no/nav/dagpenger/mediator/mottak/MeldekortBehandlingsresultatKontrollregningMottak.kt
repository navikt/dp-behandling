package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mediator.api.models.AvgjørelseDTO
import no.nav.dagpenger.mediator.api.models.OpprinnelseDTO
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidsdagId
import no.nav.dagpenger.regel.OpplysningsTyper.arbeidstimerId
import no.nav.dagpenger.regel.OpplysningsTyper.trekkVedForsenMeldingId
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.vilkår.Meldeplikt.oppfyllerMeldeplikt
import no.nav.dagpenger.regelverk.hendelseTypeOpplysningstype
import tools.jackson.databind.JsonNode
import java.math.BigDecimal
import java.util.UUID

class MeldekortBehandlingsresultatKontrollregningMottak(
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
        val kontrollbehov = MeldekortberegningKontrollbehov(packet)

        withLoggingContext("behandlingId" to packet["behandlingId"].asString()) {
            if (!kontrollbehov.harKontrollbehov) {
                logger.info {
                    "Ingen kontrollbehov: " +
                        "meldekortMedInnhold=${kontrollbehov.meldekortMedInnhold}, " +
                        "trekkVedForSenMelding=${kontrollbehov.harTrekkVedForSenMelding}, " +
                        "harEndring=${kontrollbehov.harEndring}, " +
                        "harStans=${kontrollbehov.harStans}"
                }
                return
            }

            val detaljer = kontrollbehov.kontrollbehovDetaljer()
            context.publish(
                packet["ident"].asString(),
                JsonMessage
                    .newMessage(
                        "meldekortberegning_trenger_kontrollregning",
                        mapOf(
                            "ident" to packet["ident"].asString(),
                            "behandlingId" to packet["behandlingId"].asString(),
                            "behandletHendelseId" to packet["behandletHendelse"]["id"].asString(),
                            "detaljer" to detaljer,
                        ),
                    ).toJson(),
            )
            logger.info { "Publiserte kontrollregningbehov: $detaljer" }
        }
    }

    private class MeldekortberegningKontrollbehov(
        private val packet: JsonMessage,
    ) {
        val harArbeidsdagMedFalse get() = nyePerioder.any { it.er(arbeidsdagId) && it.periode.boolskVerdi(erLik = false) }
        val harArbeidstimerIkkeNull
            get() =
                nyePerioder.any {
                    it.er(arbeidstimerId) && !it.periode.desimaltallVerdi(erLik = BigDecimal.ZERO)
                }
        val harNyOpplysningUtenforBeregning get() = nyePerioder.any { it.opplysningTypeId !in beregningOpplysningTypeIder }

        val harTrekkVedForSenMelding
            get() =
                nyePerioder.any {
                    it.er(trekkVedForsenMeldingId) && it.periode.boolskVerdi(erLik = false)
                }
        val meldekortMedInnhold
            get() = harArbeidsdagMedFalse || harArbeidstimerIkkeNull

        val harEndring get() = harNyOpplysningUtenforBeregning
        val harStans get() = packet["førteTil"].er(AvgjørelseDTO.STANS)

        val harKontrollbehov get() = (harTrekkVedForSenMelding || meldekortMedInnhold) && (harEndring || harStans)

        fun kontrollbehovDetaljer() =
            mapOf(
                "trekkVedForsenMelding" to harTrekkVedForSenMelding,
                "arbeidsdagUtenArbeid" to harArbeidsdagMedFalse,
                "arbeidstimerIkkeNull" to harArbeidstimerIkkeNull,
                "avgjorelseStans" to harStans,
                "nyOpplysningUtenforBeregning" to harNyOpplysningUtenforBeregning,
            )

        private val nyePerioder: List<NyPeriode> by lazy {
            packet["opplysninger"]
                .toList()
                .filterNot {
                    it["opplysningTypeId"].asString() == hendelseTypeOpplysningstype.id.uuid.toString()
                }.flatMap { opplysning ->
                    val opplysningTypeId = opplysning["opplysningTypeId"].asUUIDOrNull() ?: return@flatMap emptyList()
                    opplysning["perioder"]
                        .toList()
                        .filter { it.erNyPeriode() }
                        .map { NyPeriode(opplysningTypeId, it) }
                }
        }

        private fun NyPeriode.er(opplysningTypeId: Opplysningstype.Id<*>) = this.opplysningTypeId == opplysningTypeId.uuid

        private companion object {
            private val ekstraOpplysninger = setOf(oppfyllerMeldeplikt)
            private val beregningOpplysningTypeIder =
                Beregning.regelsett.produserer
                    .map { it.id.uuid }
                    .toSet() + ekstraOpplysninger

            private fun JsonNode.erNyPeriode() = this["opprinnelse"].er(OpprinnelseDTO.NY)

            private fun JsonNode.er(enumVerdi: Enum<*>) =
                this.asString().let { råverdi ->
                    råverdi.equals(enumVerdi.name, ignoreCase = true) || råverdi.equals(enumVerdi.toString(), ignoreCase = true)
                }

            private fun JsonNode.boolskVerdi(erLik: Boolean): Boolean {
                val verdiNode = this["verdi"]["verdi"]
                val verdi =
                    when {
                        verdiNode.isBoolean -> verdiNode.asBoolean()
                        else -> verdiNode.asString().toBooleanStrictOrNull()
                    }

                return verdi == erLik
            }

            private fun JsonNode.desimaltallVerdi(erLik: BigDecimal): Boolean {
                val verdiNode = this["verdi"]["verdi"]
                val verdi =
                    when {
                        verdiNode.isNumber -> verdiNode.decimalValue()
                        else -> verdiNode.asString().toBigDecimalOrNull()
                    }

                return verdi?.compareTo(erLik) == 0
            }

            private fun JsonNode.asUUIDOrNull() = this.asString().let { runCatching { UUID.fromString(it) }.getOrNull() }
        }
    }

    private data class NyPeriode(
        val opplysningTypeId: UUID,
        val periode: JsonNode,
    )

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
