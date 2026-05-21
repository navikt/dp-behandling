package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.opplysning.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

class BesluttBehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    val besluttetAv: Saksbehandler,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse
