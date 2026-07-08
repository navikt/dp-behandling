package no.nav.dagpenger.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mediator.api.models.OpprinnelseDTO
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.arbeidsdag
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.arbeidstimer
import no.nav.dagpenger.regel.regelsett.beregning.Beregning.meldtITide
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid.fastsattVanligArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode.harSanksjon
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.kravTilArbeidstidsreduksjon
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
                validate {
                    it.requireKey(
                        "ident",
                        "behandlingId",
                        "behandletHendelse",
                        "opplysninger",
                        "rettighetsperioder",
                    )
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val kontrollbehov = MeldekortberegningKontrollbehov(packet)

        if (!kontrollbehov.harKontrollbehov) return

        withLoggingContext("behandlingId" to packet["behandlingId"].asString()) {
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
        val harKontrollbehov get() = (meldekortSendtForSent || meldekortDerNoeErMeldt) && harEndring

        private val meldekortSendtForSent get() = nyePerioder(meldtITide).any { it.medVerdi(false) }
        private val meldekortDerNoeErMeldt get() = harArbeidsdagMedFalse || harMeldtArbeidstimer

        private val harEndring
            get() =
                // Endring i sats (eks barnetillegg eller samordning)
                harEndringISats ||
                    // Endring i arbeidstid (bruker godkjennes som deltidsarbeidssøker eller samordning)
                    harEndringiArbeidstid ||
                    // Endring i terskel (fiskepermittering)
                    harEndringITerskel ||
                    // Ileggelse av sanksjon i løpende sak
                    ileggesSanksjon ||
                        /*
                         * Stans av dagpengesak som følge av manuell behandling (bruker opplyser for eksempel i modia at hen har startet i jobb)
                         * Stans av dagpengesak (automatisk) som følge av at bruker krysser nei på spørsmål om videre tilmelding
                         * Stans av dagpengesak (automatisk) som følge av manglende innsending av meldekort
                         * Stans av dagpengesak (automatisk) som følge av maksimal stønadsperiode nådd
                         * Gjenopptak i samme periode som man har fått stansvedtak
                         */
                    harEndretRettighetsperiode

        fun kontrollbehovDetaljer() =
            mapOf(
                "meldekortSendtForSent" to meldekortSendtForSent,
                "harMeldtAnnenAktivitet" to harArbeidsdagMedFalse,
                "harMeldtArbeidstimer" to harMeldtArbeidstimer,
                "harEndringISats" to harEndringISats,
                "harEndringiArbeidstid" to harEndringiArbeidstid,
                "harEndringITerskel" to harEndringITerskel,
                "ileggesSanksjon" to ileggesSanksjon,
                "harEndretRettighetsperiode" to harEndretRettighetsperiode,
            )

        // Endring i sats (eks barnetillegg eller samordning)
        private val harEndringISats get() = nyePerioder.any { it.er(dagsatsEtterSamordningMedBarnetillegg) }

        // Endring i arbeidstid (bruker godkjennes som deltidsarbeidssøker eller samordning)
        private val harEndringiArbeidstid get() = nyePerioder.any { it.er(fastsattVanligArbeidstid) }

        // Endring i terskel (fiskepermittering)
        private val harEndringITerskel get() = nyePerioder.any { it.er(kravTilArbeidstidsreduksjon) }

        private val harArbeidsdagMedFalse get() = nyePerioder(arbeidsdag).any { it.medVerdi(false) }
        private val harMeldtArbeidstimer get() = nyePerioder(arbeidstimer).any { !it.medVerdi(BigDecimal.ZERO) }

        private val ileggesSanksjon get() = nyePerioder(harSanksjon).any { it.medVerdi(true) }

        private val harEndretRettighetsperiode get() = packet["rettighetsperioder"].any { it["opprinnelse"].asString() == "Ny" }

        private val nyePerioder: List<NyPeriode> by lazy {
            packet["opplysninger"]
                .toList()
                .flatMap { opplysning ->
                    val opplysningTypeId = opplysning["opplysningTypeId"].asUUIDOrNull() ?: return@flatMap emptyList()
                    opplysning["perioder"]
                        .toList()
                        .filter { it.erNyPeriode() }
                        .map { NyPeriode(opplysningTypeId, it) }
                }
        }

        private fun nyePerioder(opplysningstype: Opplysningstype<*>) = nyePerioder.filter { it.er(opplysningstype) }

        private companion object {
            private fun JsonNode.erNyPeriode() =
                this["opprinnelse"].asString().let { råverdi ->
                    råverdi.equals(OpprinnelseDTO.NY.name, ignoreCase = true) ||
                        råverdi.equals(OpprinnelseDTO.NY.toString(), ignoreCase = true)
                }

            private fun JsonNode.asUUIDOrNull() = this.asString().let { runCatching { UUID.fromString(it) }.getOrNull() }
        }
    }

    private data class NyPeriode(
        private val opplysningTypeId: UUID,
        val periode: JsonNode,
    ) {
        fun er(opplysningType: Opplysningstype<*>) = opplysningTypeId == opplysningType.id.uuid

        fun medVerdi(verdi: Boolean) = periode["verdi"]["verdi"].asBoolean() == verdi

        fun medVerdi(erLik: BigDecimal): Boolean {
            val verdiNode = periode["verdi"]["verdi"]
            val verdi =
                when {
                    verdiNode.isNumber -> verdiNode.decimalValue()
                    else -> verdiNode.asString().toBigDecimalOrNull()
                }

            return verdi?.compareTo(erLik) == 0
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
