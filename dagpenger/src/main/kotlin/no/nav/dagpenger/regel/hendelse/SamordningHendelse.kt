package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.hendelser.SamordningId
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.IkkeOpprettet
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.Opprettet
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.prosess.Manuellprosess
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Egen StartHendelse for mulig samordning mot annen ytelse, slik at vi selv kan avgjøre om hendelsen
// skal føre til en behandling, uavhengig av OpprettBehandlingHendelse (som brukes ved manuelt opprettede behandlinger).
class SamordningHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    eksternId: SamordningId,
    gjelderDato: LocalDate,
    private val begrunnelse: String? = null,
    opprettet: LocalDateTime,
    prosess: Forretningsprosess = Manuellprosess(),
) : StartHendelse(meldingsreferanseId, ident, eksternId, gjelderDato, opprettet) {
    override val forretningsprosess = prosess

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat {
        if (rettighetstatus.harAktivRettighet(skjedde)) {
            return IkkeOpprettet("Samordningshendelse av type $type overlapper med en aktiv rettighetsperiode fra $skjedde")
        }
        if (forrigeBehandling == null) {
            return IkkeOpprettet("Hendelse av type $type kan ikke starte en ny behandlingskjede uten en tidligere behandling")
        }

        begrunnelse?.let { info("Begrunnelse for opprettelse: $it") }

        return Opprettet(
            opprettBehandling(
                basertPå = forrigeBehandling,
                avklaringer =
                    listOf(
                        Avklaring(
                            Avklaringkode(
                                kode = "MuligSamordning",
                                tittel = "Mulig samordning mot annen ytelse",
                                beskrivelse = begrunnelse ?: "Behandlingen er opprettet manuelt og kan ikke automatisk behandles",
                                kanAvbrytes = false,
                            ),
                        ),
                    ),
            ),
        )
    }

    private fun TemporalCollection<Rettighetstatus>.harAktivRettighet(dato: LocalDate) =
        runCatching { get(dato).utfall }.getOrDefault(false)
}
