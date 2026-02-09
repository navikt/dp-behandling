package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.EksternId
import no.nav.dagpenger.behandling.modell.hendelser.Hendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.Avklaringspunkter.OmgjøringBehandling
import no.nav.dagpenger.regel.Omgjøringsprosess
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class OmgjøringHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    eksternId: EksternId<*>,
    gjelderDato: LocalDate,
    opprettet: LocalDateTime,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = eksternId,
        skjedde = gjelderDato,
        opprettet = opprettet,
    ) {
    override val forretningsprosess = Omgjøringsprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        requireNotNull(forrigeBehandling) { "Omgjøring krever en tidligere behandling å basere seg på" }

        val kilde = Systemkilde(meldingsreferanseId, opprettet)

        return Behandling(
            basertPå = forrigeBehandling,
            behandler =
                Hendelse(
                    meldingsreferanseId = meldingsreferanseId,
                    type = type,
                    ident = ident,
                    eksternId = eksternId,
                    skjedde = skjedde,
                    opprettet = opprettet,
                    forretningsprosess = forretningsprosess,
                ),
            opplysninger =
                listOf(
                    Faktum(
                        hendelseTypeOpplysningstype,
                        type,
                        Gyldighetsperiode.kun(skjedde),
                        kilde = kilde,
                    ),
                ),
            avklaringer = listOf(Avklaring(OmgjøringBehandling)),
        )
    }
}
