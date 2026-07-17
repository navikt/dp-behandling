package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.hendelser.Hendelse
import no.nav.dagpenger.modell.hendelser.KlageId
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.Opprettet
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.prosess.Omgjøringsprosess
import no.nav.dagpenger.regelverk.hendelseTypeOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KlagebehandlingHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    eksternId: KlageId<*>,
    gjelderDato: LocalDate,
    opprettet: LocalDateTime,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = eksternId,
        skjedde = gjelderDato,
        opprettet = opprettet,
    ) {
    override val forretningsprosess: Forretningsprosess
        get() = Omgjøringsprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat {
        requireNotNull(forrigeBehandling) { "Klage krever en tidligere behandling å basere seg på" }

        val kilde = Systemkilde(meldingsreferanseId, opprettet)

        return Opprettet(
            Behandling(
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
                opplysninger = emptyList(),
                avklaringer =
                    listOf(
                        Avklaring(
                            Avklaringkode(
                                kode = "OmgjøringEtterKlage",
                                tittel = "Omgjøring etter klage",
                                beskrivelse = "Behandlingen er en omgjøring etter klage og kan ikke automatisk behandles",
                                kanAvbrytes = false,
                            ),
                        ),
                    ),
            ).also {
                it.opplysninger.leggTil(
                    Faktum(
                        hendelseTypeOpplysningstype,
                        type,
                        gyldighetsperiode = Gyldighetsperiode.kun(skjedde),
                        kilde = kilde,
                    ),
                )
            },
        )
    }
}
