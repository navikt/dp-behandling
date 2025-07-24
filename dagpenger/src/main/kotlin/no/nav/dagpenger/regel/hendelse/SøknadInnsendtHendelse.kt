package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.Hendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysninger.Companion.basertPå
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.OpplysningsTyper.FagsakIdId
import no.nav.dagpenger.regel.Søknadsprosess
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SøknadInnsendtHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    søknadId: UUID,
    gjelderDato: LocalDate,
    val fagsakId: Int,
    opprettet: LocalDateTime,
) : StartHendelse(meldingsreferanseId, ident, SøknadId(søknadId), gjelderDato, opprettet) {
    override val forretningsprosess = Søknadsprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        val basertPå =
            listOfNotNull(
                when (forrigeBehandling?.behandler?.type) {
                    "SøknadInnsendtHendelse" -> {
                        when (forrigeBehandling.vedtakopplysninger.utfall) {
                            // Skal IKKE kjede
                            false -> null
                            // Skal kjede
                            else -> forrigeBehandling
                        }
                    }
                    // Skal kjede
                    else -> forrigeBehandling
                },
            )

        return Behandling(
            basertPå = basertPå,
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
                buildList {
                    if (basertPå.isEmpty()) {
                        add(Faktum(fagsakIdOpplysningstype, fagsakId, kilde = Systemkilde(meldingsreferanseId, opprettet)))
                    }
                    add(Faktum(søknadIdOpplysningstype, eksternId.id.toString(), kilde = Systemkilde(meldingsreferanseId, opprettet)))
                    add(
                        Faktum(
                            hendelseTypeOpplysningstype,
                            type,
                            gyldighetsperiode = Gyldighetsperiode(fom = skjedde),
                            kilde = Systemkilde(meldingsreferanseId, opprettet),
                        ),
                    )
                },
        )
    }

    companion object {
        val fagsakIdOpplysningstype = Opplysningstype.heltall(FagsakIdId, "fagsakId")
        val hendelseTypeOpplysningstype = Opplysningstype.tekst(OpplysningsTyper.HendelseTypeId, "hendelseType")
    }
}
