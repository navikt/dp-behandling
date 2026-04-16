package no.nav.dagpenger.regel.hendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.FerietilleggId
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.Ferietilleggprosess
import no.nav.dagpenger.regel.KravPåFerietillegg
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
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
    ): Behandling {
        val kilde = Systemkilde(meldingsreferanseId, opprettet)
        return Behandling(
            basertPå = null,
            behandler = this,
            opplysninger =
                listOf(
                    Faktum(
                        hendelseTypeOpplysningstype,
                        type,
                        Gyldighetsperiode.kun(skjedde),
                        kilde = kilde,
                    ),
                    Faktum(
                        KravPåFerietillegg.åretDetSkalBeregnesFerietilleggFor,
                        opptjeningsår,
                        Gyldighetsperiode(
                            fraOgMed = LocalDate.of(opptjeningsår, 1, 1),
                            tilOgMed = LocalDate.of(opptjeningsår, 12, 31),
                        ),
                        kilde = kilde,
                    ),
                ),
            // husk å legge inn avklaring som stopper disse opp så saksbehandler kan sjekke de
            avklaringer = emptyList(),
        )
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
