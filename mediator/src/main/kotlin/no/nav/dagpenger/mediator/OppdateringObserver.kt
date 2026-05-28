package no.nav.dagpenger.mediator

import no.nav.dagpenger.mediator.repository.NyOppdatering
import no.nav.dagpenger.mediator.repository.OppdateringRepository
import no.nav.dagpenger.modell.BehandlingObservatør
import no.nav.dagpenger.modell.BehandlingObservatør.AvklaringLukket
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingAvbrutt
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingEndretTilstand
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingForslagTilVedtak
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingOpprettet
import no.nav.dagpenger.modell.PersonObservatør
import java.security.MessageDigest
import java.util.UUID

internal class OppdateringObserver : PersonObservatør {
    private val oppdateringer = mutableListOf<PendingOppdatering>()

    override fun opprettet(event: BehandlingOpprettet) {
        oppdateringer.add(
            event.tilOppdatering(
                type = "behandling_opprettet",
                payload =
                    mapOf(
                        "ident" to requireNotNull(event.ident),
                        "behandlingId" to event.behandlingId,
                        "behandlingskjedeId" to event.behandlingskjedeId,
                        "basertPåBehandling" to event.basertPåBehandlinger,
                        "regelverk" to event.regelverk.navn,
                        "hendelseType" to event.hendelse.eksternId.datatype,
                    ),
            ),
        )
    }

    override fun endretTilstand(event: BehandlingEndretTilstand) {
        oppdateringer.add(
            event.tilOppdatering(
                type = "behandling_endret_tilstand",
                payload =
                    mapOf(
                        "ident" to requireNotNull(event.ident),
                        "behandlingId" to event.behandlingId,
                        "forrigeTilstand" to event.forrigeTilstand.name,
                        "gjeldendeTilstand" to event.gjeldendeTilstand.name,
                    ),
            ),
        )
    }

    override fun forslagTilVedtak(event: BehandlingForslagTilVedtak) {
        oppdateringer.add(event.tilVedtakOppdatering("forslag_til_behandlingsresultat"))
    }

    override fun ferdig(event: BehandlingFerdig) {
        oppdateringer.add(event.tilVedtakOppdatering("behandlingsresultat"))
    }

    override fun avbrutt(event: BehandlingAvbrutt) {
        oppdateringer.add(
            event.tilOppdatering(
                type = "behandling_avbrutt",
                payload =
                    mapOf(
                        "ident" to requireNotNull(event.ident),
                        "behandlingId" to event.behandlingId,
                        "årsak" to event.årsak,
                        "hendelseType" to event.hendelse.datatype,
                    ),
            ),
        )
    }

    override fun avklaringLukket(event: AvklaringLukket) {
        oppdateringer.add(
            event.tilOppdatering(
                type = "avklaring_lukket",
                payload =
                    mapOf(
                        "ident" to requireNotNull(event.ident),
                        "behandlingId" to event.behandlingId,
                        "avklaringId" to event.avklaringId,
                        "kode" to event.kode,
                    ),
            ),
        )
    }

    internal fun ferdigstill(
        repository: OppdateringRepository,
        hendelseId: UUID,
    ) {
        repository.lagre(oppdateringer.map { it.tilNyOppdatering(hendelseId) })
        oppdateringer.clear()
    }

    private fun PersonObservatør.PersonEvent.tilOppdatering(
        type: String,
        payload: Map<String, Any?>,
    ): PendingOppdatering {
        val payloadJson = objectMapper.writeValueAsString(payload)
        return PendingOppdatering(
            ident = requireNotNull(ident),
            behandlingId = payload["behandlingId"] as? UUID,
            type = type,
            payload = payloadJson,
        )
    }

    private fun PendingOppdatering.tilNyOppdatering(hendelseId: UUID) =
        NyOppdatering(
            hendelseId = hendelseId,
            ident = ident,
            behandlingId = behandlingId,
            type = type,
            payload = payload,
            payloadHash = payload.sha256(),
        )

    private fun String.sha256(): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }

    private data class PendingOppdatering(
        val ident: String,
        val behandlingId: UUID?,
        val type: String,
        val payload: String,
    )

    private fun BehandlingObservatør.VedtakEvent.tilVedtakOppdatering(type: String) =
        PendingOppdatering(
            ident = requireNotNull(ident),
            behandlingId = behandlingId,
            type = type,
            payload =
                objectMapper.writeValueAsString(
                    mapOf(
                        "ident" to requireNotNull(ident),
                        "behandlingId" to behandlingId,
                        "behandlingskjedeId" to behandlingskjedeId,
                        "avgjørelse" to avgjørelse.toString(),
                        "automatiskBehandlet" to automatiskBehandlet,
                        "opprettet" to opprettet,
                        "sistEndret" to sistEndret,
                    ),
                ),
        )
}
