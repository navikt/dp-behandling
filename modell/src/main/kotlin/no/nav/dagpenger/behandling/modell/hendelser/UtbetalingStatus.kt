package no.nav.dagpenger.behandling.modell.hendelser

import java.time.LocalDateTime
import java.util.UUID

class UtbetalingStatus(
    meldingsreferanseId: UUID,
    ident: String,
    val status: Status,
    val eksternMeldekortId: MeldekortId,
    override val behandlingId: UUID,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse {
    enum class Status {
        MOTTATT,
        SENDT,
        FEILET,
        UTFØRT,
    }
}
