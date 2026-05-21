package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.opplysning.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

class GodkjennBehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    val godkjentAv: Saksbehandler,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse
