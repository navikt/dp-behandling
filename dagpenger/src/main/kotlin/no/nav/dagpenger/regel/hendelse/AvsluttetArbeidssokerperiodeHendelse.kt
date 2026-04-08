package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.OmgjøringId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.Omgjøringsprosess
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AvsluttetArbeidssokerperiodeHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    val avsluttetTidspunkt: LocalDateTime,
    val fastsattMeldingsdag: LocalDate? = null,
    opprettet: LocalDateTime,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = OmgjøringId(UUIDv7.ny()),
        skjedde = fastsattMeldingsdag ?: avsluttetTidspunkt.toLocalDate(),
        opprettet = opprettet,
    ) {
    override val forretningsprosess: Forretningsprosess
        get() = Omgjøringsprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling? {
        // avsluttetTidspunkt == § 4-5 Ikke registrert
        // fastsattMeldingsdag == null -> Avklaring ?
        // fastattMeldingsdag != null --> Vi kan behandle
        // Hvis det ikke finnes en _FERDIG_ behandling --> Generell oppgave?

        TODO("Not yet implemented")
    }
}
