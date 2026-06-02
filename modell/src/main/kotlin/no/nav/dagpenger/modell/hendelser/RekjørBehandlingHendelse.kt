package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.opplysning.Opplysningstype
import java.time.LocalDateTime
import java.util.UUID

class RekjørBehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    override val behandlingId: UUID,
    opprettet: LocalDateTime,
    val oppfriskOpplysningIder: List<Opplysningstype<*>> = emptyList(),
) : PersonHendelse(meldingsreferanseId, ident, opprettet),
    BehandlingHendelse
