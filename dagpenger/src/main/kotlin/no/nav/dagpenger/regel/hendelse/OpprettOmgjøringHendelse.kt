package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.EksternId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.Manuellprosess
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OpprettOmgjøringHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    eksternId: EksternId<*>,
    gjelderDato: LocalDate,
    opprettet: LocalDateTime,
    private val behandlingId: UUID,
) : StartHendelse(meldingsreferanseId, ident, eksternId, gjelderDato, opprettet) {
    override val forretningsprosess = Manuellprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        val omgjøres =
            finnBehandlingMedId(forrigeBehandling, behandlingId)
                ?: throw IllegalStateException("Fant ikke behandling med id $behandlingId")

        // TODO: Gjøre noe sånt? omgjøres.nåBlirDuOmgjort()

        return Behandling(
            basertPå = forrigeBehandling,
            behandler = this.somHendelse(),
            opplysninger =
                listOf(
                    Faktum(
                        hendelseTypeOpplysningstype,
                        type,
                        gyldighetsperiode = Gyldighetsperiode.kun(skjedde),
                        kilde = Systemkilde(meldingsreferanseId, opprettet),
                    ),
                    // TODO: Lag noe spor av behandlingId som omgjøres
                ),
            avklaringer =
                listOf(
                    Avklaring(
                        Avklaringkode(
                            kode = "Omgjøring",
                            tittel = "Omgjøring av tidligere behandling",
                            beskrivelse = "Behandlingen er opprettet for å omgjøre tidligere behandling og kan ikke automatisk behandles",
                            kanAvbrytes = false,
                        ),
                    ),
                ),
        )
    }

    private tailrec fun finnBehandlingMedId(
        behandling: Behandling?,
        søktId: UUID,
    ): Behandling? {
        if (behandling == null) return null
        if (behandling.behandlingId == søktId) return behandling
        return finnBehandlingMedId(behandling.basertPå, søktId)
    }
}
