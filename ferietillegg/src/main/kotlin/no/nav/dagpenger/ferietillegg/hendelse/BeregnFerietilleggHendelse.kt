package no.nav.dagpenger.ferietillegg.hendelse

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.FerietilleggId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelseResultat.Opprettet
import no.nav.dagpenger.ferietillegg.Ferietilleggprosess
import no.nav.dagpenger.ferietillegg.KravPåFerietillegg
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regelverk.hendelseTypeOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BeregnFerietilleggHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    opprettet: LocalDateTime,
    private val opptjeningsår: Int,
    ferietilleggId: UUID,
) : StartHendelse(
        meldingsreferanseId = meldingsreferanseId,
        ident = ident,
        eksternId = FerietilleggId(ferietilleggId),
        skjedde = opprettet.toLocalDate(),
        opprettet = opprettet,
    ) {
    override val forretningsprosess get() = Ferietilleggprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): StartHendelseResultat {
        val kilde = Systemkilde(meldingsreferanseId, opprettet)
        return Opprettet(
            Behandling(
                basertPå = if (sammeOpptjeningsår(forrigeBehandling)) forrigeBehandling else null,
                behandler = this,
                opplysninger =
                    listOf(
                        Faktum(
                            hendelseTypeOpplysningstype,
                            type,
                            Gyldighetsperiode.kun(skjedde),
                            kilde = kilde,
                        ),
                    ),
                avklaringer = emptyList(),
            ).apply {
                this.opplysninger.leggTil(
                    Faktum(
                        KravPåFerietillegg.åretDetSkalBeregnesFerietilleggFor,
                        opptjeningsår,
                        Gyldighetsperiode(
                            fraOgMed = LocalDate.of(opptjeningsår, 1, 1),
                        ),
                        kilde = kilde,
                    ),
                )
            },
        )
    }

    private fun sammeOpptjeningsår(forrigeBehandling: Behandling?): Boolean {
        if (forrigeBehandling == null) return false
        val forrigeOpptjeningsår =
            forrigeBehandling
                .opplysninger()
                .finnOpplysning(KravPåFerietillegg.åretDetSkalBeregnesFerietilleggFor)
                .verdi
        return forrigeOpptjeningsår == opptjeningsår
    }
}
