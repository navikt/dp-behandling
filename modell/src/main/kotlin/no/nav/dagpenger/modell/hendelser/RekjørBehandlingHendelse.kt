package no.nav.dagpenger.modell.hendelser

import java.time.LocalDateTime
import java.util.UUID

class RekjørBehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    opprettet: LocalDateTime,
    val oppfriskOpplysningIder: List<UUID> = emptyList(),
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse
