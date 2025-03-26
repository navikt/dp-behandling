package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Regelverkstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Hendelse<T : Regelverkstype>(
    meldingsreferanseId: UUID,
    override val type: String,
    ident: String,
    eksternId: EksternId<*>,
    skjedde: LocalDate,
    opprettet: LocalDateTime,
    override val forretningsprosess: Forretningsprosess<T>,
) : StartHendelse<T>(meldingsreferanseId, ident, eksternId, skjedde, opprettet),
    Forretningsprosess<T> by forretningsprosess {
    override fun behandling(forrigeBehandling: Behandling<Regelverkstype>?): Behandling<Regelverkstype> =
        throw IllegalStateException("Skal ikke opprettet behandling her, skal allerede ha skjedd")
}
