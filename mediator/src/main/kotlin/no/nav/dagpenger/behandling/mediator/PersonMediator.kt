package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import mu.KotlinLogging
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

    override fun forslagTilVedtak(event: BehandlingForslagTilVedtak) {
        val ident = requireNotNull(event.ident) { "Mangler ident i ForslagTilVedtak" }
        meldinger.add(ident to event.toJsonMessage())
    }

    override fun endretTilstand(event: BehandlingEndretTilstand) {
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingEndretTilstand" }
        meldinger.add(ident to event.toJsonMessage())
    }

    override fun ferdig(event: BehandlingFerdig) {
        val ident = requireNotNull(event.ident) { "Mangler ident i BehandlingFerdig" }
        meldinger.add(ident to event.toJsonMessage())
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
        meldinger.forEach {
            context.publish(it.first, it.second.toJson())
            sikkerlogg.info { "Publisert melding. Innhold: ${it.second.toJson()}" }
        }
    }

    private fun BehandlingOpprettet.toJsonMessage() =
        JsonMessage
            .newMessage(
                "behandling_opprettet",
                mapOf(
                    "ident" to requireNotNull(ident) { "Mangler ident i BehandlingOpprettet" },
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

    private fun BehandlingForslagTilVedtak.toJsonMessage(): JsonMessage {
        val ident = Ident(requireNotNull(ident) { "Mangler ident i BehandlingForslagTilVedtak" })
        val vedtak = this.lagVedtakDTO(ident)
        return JsonMessage.newMessage("forslag_til_vedtak", vedtak.toMap())
    }

    private fun BehandlingFerdig.toJsonMessage(): JsonMessage {
        val ident = Ident(requireNotNull(ident) { "Mangler ident i BehandlingForslagTilVedtak" })
        val vedtak = this.lagVedtakDTO(ident)
        return JsonMessage.newMessage("vedtak_fattet", vedtak.toMap())
    }

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
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.PersonMediator")
    }
}
