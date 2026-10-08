package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Rettighetsperioder
import no.nav.dagpenger.opplysning.Aktør
import no.nav.dagpenger.opplysning.Forretningsprosess
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
    opprettetAv: Aktør? = null,
) : StartHendelse(meldingsreferanseId, ident, eksternId, skjedde, opprettet, opprettetAv) {
    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetsperioder: Rettighetsperioder,
    ): StartHendelseResultat = throw IllegalStateException("Skal ikke opprettet behandling her, skal allerede ha skjedd")
}
