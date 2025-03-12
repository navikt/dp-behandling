package no.nav.dagpenger.behandling.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import mu.KotlinLogging
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.behandling.modell.BehandlingObservatør.BehandlingForslagTilVedtak
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.PersonObservatør
import no.nav.dagpenger.behandling.modell.hendelser.PersonHendelse

typealias Hendelse = Pair<String, JsonMessage>

internal class PersonMediator(
    private val hendelse: PersonHendelse,
) : PersonObservatør {
    private val meldinger = mutableListOf<Hendelse>()

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

    internal fun ferdigstill(context: MessageContext) {
        meldinger.forEach {
            context.publish(it.first, it.second.toJson())
            sikkerlogg.info { "Publisert melding. Innhold: ${it.second.toJson()}" }
        }
    }

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
        val vedtak = lagVedtak(behandlingId, forrigeBehandlingId, ident, hendelse, opplysninger, automatiskBehandlet, godkjent, besluttet)
        return JsonMessage.newMessage("forslag_til_vedtak", vedtak.toMap())
    }

    private fun BehandlingFerdig.toJsonMessage(): JsonMessage {
        val ident = Ident(requireNotNull(ident) { "Mangler ident i BehandlingEndretTilstand" })
        val vedtak = lagVedtak(behandlingId, forrigeBehandlingId, ident, hendelse, opplysninger, automatiskBehandlet, godkjent, besluttet)
        return JsonMessage.newMessage("vedtak_fattet", vedtak.toMap())
    }

    private companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.PersonMediator")
    }
}
