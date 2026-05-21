package no.nav.dagpenger.modell.hendelser

import java.time.LocalDateTime
import java.util.UUID

class SendTilbakeHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse
