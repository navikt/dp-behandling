package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.Rettighetstatus.Companion.harIkkeInnvilgelse
import no.nav.dagpenger.modell.hendelser.EksternId
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

class OpprettBehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    eksternId: EksternId<*>,
    gjelderDato: LocalDate,
    private val begrunnelse: String? = null,
    opprettet: LocalDateTime,
    private val startNyKjede: Boolean = true,
    prosess: Forretningsprosess = Manuellprosess(),
) : StartHendelse(meldingsreferanseId, ident, eksternId, gjelderDato, opprettet) {
    override val forretningsprosess = prosess

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat {
        if (forrigeBehandling == null && !startNyKjede) {
            return IkkeOpprettet("Hendelse av type $type kan ikke starte en ny behandlingskjede uten en tidligere behandling")
        }
        if (eksternId is SamordningId && rettighetstatus.harIkkeInnvilgelse) {
            return IkkeOpprettet("Samordningshendelse av type $type kan ikke opprette behandling uten innvilget dagpengerett")
        }

        begrunnelse?.let { info("Begrunnelse for opprettelse: $it") }

        return Opprettet(
            opprettBehandling(
                basertPå = forrigeBehandling,
                avklaringer =
                    listOf(
                        Avklaring(
                            Avklaringkode(
                                kode = "ManuellBehandling",
                                tittel = "Manuell behandling",
                                beskrivelse = begrunnelse ?: "Behandlingen er opprettet manuelt og kan ikke automatisk behandles",
                                kanAvbrytes = false,
                            ),
                        ),
                    ),
            ),
        )
    }
}
