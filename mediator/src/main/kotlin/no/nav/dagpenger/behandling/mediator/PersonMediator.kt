package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.behandling.mediator.api.tilBehandlingsresultatDTO
import no.nav.dagpenger.behandling.modell.BehandlingObservatør
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.AvklaringLukket
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingAvbrutt
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingForslagTilVedtak
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingOpprettet
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.PersonObservatør
import no.nav.dagpenger.behandling.modell.hendelser.ManuellId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId

typealias Hendelse = Pair<String, JsonMessage>

internal class PersonMediator : PersonObservatør {
    private val meldinger = mutableListOf<Hendelse>()

    override fun opprettet(event: BehandlingOpprettet) {
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingOpprettet" }
        meldinger.add(ident to event.toJsonMessage())
    }

    override fun endretTilstand(event: BehandlingEndretTilstand) {
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingEndretTilstand" }
        meldinger.add(ident to event.toJsonMessage())
    }

    override fun forslagTilVedtak(event: BehandlingForslagTilVedtak) {
        val ident = requireNotNull(event.ident) { "Mangler ident i ForslagTilVedtak" }
        meldinger.add(ident to event.tilBehandlingsresultat("forslag_til_behandlingsresultat", ident))
    }

    override fun ferdig(event: BehandlingFerdig) {
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingFerdig" }
        meldinger.add(ident to event.tilBehandlingsresultat("behandlingsresultat", ident))

        if (event.rettighetsperioder.size == 1 && event.rettighetsperioder.single().harRett == false) {
            meldinger.add(ident to event.tilVedtakFattetMelding())
        }
    }

    override fun avbrutt(event: BehandlingAvbrutt) {
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingFerdig" }
        meldinger.add(ident to event.toJsonMessage())
    }

    override fun avklaringLukket(event: AvklaringLukket) {
        val ident = requireNotNull(event.ident) { "Mangler ident i AvklaringLukket" }
        meldinger.add(ident to event.toJsonMessage())
    }

    internal fun ferdigstill(context: MessageContext) {
        meldinger.forEach { (key, melding) ->
            context.publish(key, melding.toJson())
            withLoggingContext("meldingId" to melding.id) {
                logger.info { "Publisert melding." }
                sikkerlogg.info { "Publisert melding. Innhold: ${melding.toJson()}" }
            }
        }
    }

    private fun BehandlingOpprettet.toJsonMessage() =
        JsonMessage
            .newMessage(
                "behandling_opprettet",
                mapOf(
                    "ident" to requireNotNull(ident) { "Mangler ident i BehandlingOpprettet" },
                    "behandlingId" to behandlingId.toString(),
                    "basertPåBehandlinger" to listOf(basertPåBehandlinger.toString()),
                    "behandlingskjedeId" to behandlingskjedeId.toString(),
                    "behandletHendelse" to
                        mapOf(
                            "id" to hendelse.eksternId.id,
                            "datatype" to hendelse.eksternId.datatype,
                            "type" to
                                when (hendelse.eksternId) {
                                    is MeldekortId -> "Meldekort"
                                    is SøknadId -> "Søknad"
                                    is ManuellId -> "Manuell"
                                },
                            "skjedde" to hendelse.skjedde,
                        ),
                ) +
                    listOfNotNull(
                        basertPåBehandlinger?.let { "basertPåBehandling" to it.toString() },
                    ),
            )

    private fun BehandlingEndretTilstand.toJsonMessage() =
        JsonMessage
            .newMessage(
                "behandling_endret_tilstand",
                mapOf(
                    "ident" to requireNotNull(ident) { "Mangler ident i BehandlingEndretTilstand" },
                    "behandlingId" to behandlingId.toString(),
                    "forrigeTilstand" to forrigeTilstand.name,
                    "gjeldendeTilstand" to gjeldendeTilstand.name,
                    "forventetFerdig" to forventetFerdig.toString(),
                    "tidBrukt" to tidBrukt.toString(),
                ),
            )

    private fun BehandlingFerdig.tilVedtakFattetMelding(): JsonMessage {
        val ident = Ident(requireNotNull(ident) { "Mangler ident i BehandlingForslagTilVedtak" })
        val vedtak = this.lagVedtakDTO(ident)
        return JsonMessage.newMessage("vedtak_fattet", toMap(vedtak))
    }

    private fun BehandlingObservatør.VedtakEvent.tilBehandlingsresultat(
        hendelseNavn: String,
        ident: String,
    ) = JsonMessage.newMessage(hendelseNavn, toMap(tilBehandlingsresultatDTO(ident)))

    private fun BehandlingAvbrutt.toJsonMessage() =
        JsonMessage
            .newMessage(
                "behandling_avbrutt",
                mapOf(
                    "ident" to requireNotNull(ident) { "Mangler ident i BehandlingAvbrutt" },
                    "behandlingId" to behandlingId.toString(),
                    "behandletHendelse" to
                        mapOf(
                            "id" to hendelse.id,
                            "datatype" to hendelse.datatype,
                            "type" to
                                when (hendelse) {
                                    is MeldekortId -> "Meldekort"
                                    is SøknadId -> "Søknad"
                                    is ManuellId -> "Manuell"
                                },
                        ),
                ) + (årsak?.let { mapOf("årsak" to it) } ?: emptyMap()),
            )

    private fun AvklaringLukket.toJsonMessage() =
        JsonMessage
            .newMessage(
                "avklaring_lukket",
                mapOf(
                    "ident" to requireNotNull(ident) { "Mangler ident i AvklaringLukket" },
                    "behandlingId" to behandlingId.toString(),
                    "avklaringId" to avklaringId.toString(),
                    "kode" to kode,
                ),
            )

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.PersonMediator")
    }
}
