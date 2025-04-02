package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.TemporalCollection
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Hendelse(
    meldingsreferanseId: UUID,
    override val type: String,
    ident: String,
    eksternId: EksternId<*>,
    skjedde: LocalDate,
    opprettet: LocalDateTime,
    override val forretningsprosess: Forretningsprosess,
) : StartHendelse(meldingsreferanseId, ident, eksternId, skjedde, opprettet),
    Forretningsprosess by forretningsprosess {
    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling = throw IllegalStateException("Skal ikke opprettet behandling her, skal allerede ha skjedd")
}
