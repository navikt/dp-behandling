package no.nav.dagpenger.regel.hendelse

import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.behandling.modell.hendelser.Hendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.Avklaringspunkter.GjenopptakBehandling
import no.nav.dagpenger.regel.Avklaringspunkter.GjenopptakKanIkkeSkrivesTilbake
import no.nav.dagpenger.regel.Avklaringspunkter.SøktGjenopptak
import no.nav.dagpenger.regel.OpplysningsTyper
import no.nav.dagpenger.regel.OpplysningsTyper.FagsakIdId
import no.nav.dagpenger.regel.Søknadsprosess
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class Søknadstype {
    NySøknad,
    Gjenopptak,
}

class SøknadInnsendtHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    søknadId: UUID,
    gjelderDato: LocalDate,
    val fagsakId: Int,
    opprettet: LocalDateTime,
    val søknadstype: Søknadstype,
) : StartHendelse(meldingsreferanseId, ident, SøknadId(søknadId), gjelderDato, opprettet) {
    override val forretningsprosess = Søknadsprosess()

    override fun behandling(
        forrigeBehandling: Behandling?,
        rettighetstatus: TemporalCollection<Rettighetstatus>,
    ): Behandling {
        val basertPå =
            forrigeBehandling?.let { forrigeBehandling ->
                val erSammeType = forrigeBehandling.behandler.erSammeType(this)
                val rettighetsperioder = forrigeBehandling.vedtakopplysninger.rettighetsperioder

                // Gamle behandlinger mangler rettighetsperioder
                // Da skal vi ikke kjede
                if (rettighetsperioder.isEmpty()) {
                    return@let null
                }

                // Forrige behandling var avslag
                val varAvslag = rettighetsperioder.size == 1 && !rettighetsperioder.single().harRett

                if (erSammeType && varAvslag) {
                    return@let null
                }

                forrigeBehandling
            }

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
                    if (basertPå == null && fagsakId != 0) {
                        add(Faktum(fagsakIdOpplysningstype, fagsakId, kilde = Systemkilde(meldingsreferanseId, opprettet)))
                    }
                    add(Faktum(søknadIdOpplysningstype, eksternId.id.toString(), kilde = Systemkilde(meldingsreferanseId, opprettet)))
                    add(
                        Faktum(
                            hendelseTypeOpplysningstype,
                            type,
                            gyldighetsperiode = Gyldighetsperiode.kun(skjedde),
                            kilde = Systemkilde(meldingsreferanseId, opprettet),
                        ),
                    )
                },
            avklaringer =
                buildList {
                    if (basertPå != null) {
                        add(Avklaring(GjenopptakBehandling))
                    }
                    if (søknadstype == Søknadstype.Gjenopptak) {
                        add(Avklaring(SøktGjenopptak))
                    }
                    if (basertPå == null && fagsakId == 0) {
                        add(Avklaring(GjenopptakKanIkkeSkrivesTilbake))
                    }
                },
        )
    }

    companion object {
        val fagsakIdOpplysningstype = Opplysningstype.heltall(FagsakIdId, "fagsakId")
        val hendelseTypeOpplysningstype = Opplysningstype.tekst(OpplysningsTyper.HendelseTypeId, "hendelseType")
    }
}
