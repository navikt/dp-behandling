package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.opplysning.Saksbehandler
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import java.time.LocalDateTime
import java.util.UUID

class AvklaringKvittertHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    val avklaringId: UUID,
    override val behandlingId: UUID,
    saksbehandler: String,
    val begrunnelse: String,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse {
    override fun kontekstMap(): Map<String, String> =
        mapOf(
            "behandlingId" to behandlingId.toString(),
            "avklaringId" to avklaringId.toString(),
        )

    val kilde =
        Saksbehandlerkilde(
            meldingsreferanseId = meldingsreferanseId,
            saksbehandler = Saksbehandler(saksbehandler),
            opprettet = opprettet,
        )
}
