package no.nav.dagpenger.behandling.modell.hendelser

import java.util.UUID

class AvbrytBehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
) : PersonHendelse(meldingsreferanseId, ident), BehandlingHendelse
