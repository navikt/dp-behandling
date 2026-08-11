package no.nav.dagpenger.modell.hendelser

import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Opplysninger
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
    // Opplysningene behandlingen ble opprettet med, se StartHendelse.opprettBehandling(). Brukes til å kunne
    // resette en behandling tilbake til utgangspunktet, f.eks. om en saksbehandler har fjernet en opplysning
    // (som søknadId) den ikke skulle fjernet, eller ved rehydrering av behandlingen. Er en uavhengig kopi
    // (se Opplysninger.kopiAv), satt av StartHendelse.opprettBehandling() etter at Behandlingen er opprettet –
    // ikke ment å endres av andre.
    override var opplysninger: Opplysninger = Opplysninger(),
) : StartHendelse(meldingsreferanseId, ident, eksternId, skjedde, opprettet) {
    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat = throw IllegalStateException("Skal ikke opprettet behandling her, skal allerede ha skjedd")
}
