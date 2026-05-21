package no.nav.dagpenger.modell.hendelser

import java.time.LocalDateTime
import java.util.UUID

class AvklaringBehandletHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    val behandlingId: UUID,
    val behandlesManuelt: Boolean,
    val avklaringer: List<Avklaring>,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet) {
    data class Avklaring(
        val type: String,
        val utfall: String,
        val begrunnelse: String,
    )
}
