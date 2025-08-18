package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.opplysning.Systemkilde
import java.time.LocalDateTime
import java.util.UUID

class AvklaringIkkeRelevantHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    val avklaringId: UUID,
    val kode: String,
    override val behandlingId: UUID,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse {
    override fun kontekstMap(): Map<String, String> =
        mapOf(
            "behandlingId" to behandlingId.toString(),
            "avklaringId" to avklaringId.toString(),
            "kode" to kode,
        )

    val kilde =
        Systemkilde(
            meldingsreferanseId = meldingsreferanseId,
            opprettet = opprettet,
            registrert = LocalDateTime.now(),
        )
}
