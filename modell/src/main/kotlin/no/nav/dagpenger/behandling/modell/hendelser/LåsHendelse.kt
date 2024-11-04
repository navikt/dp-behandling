package no.nav.dagpenger.behandling.modell.hendelser

import java.time.LocalDateTime
import java.util.UUID

class LåsHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse
