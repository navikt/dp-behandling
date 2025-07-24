package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.behandling.modell.Behandling
import java.time.LocalDateTime
import java.util.UUID

class FlyttBehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    val nyBasertPåId: UUID? = null,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse {
    // TODO: VIL VI VIRKELIG DETTE?
    var nyBasertPå: Behandling? = null

    override fun kontekstMap(): Map<String, String> =
        mapOf(
            "behandlingId" to behandlingId.toString(),
        )
}
