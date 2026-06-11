package no.nav.dagpenger.regel.hendelse
import no.nav.dagpenger.avklaring.Avklaring
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.modell.hendelser.Hendelse
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.IkkeOpprettet
import no.nav.dagpenger.modell.hendelser.StartHendelseResultat.Opprettet
import no.nav.dagpenger.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.regel.Avklaringspunkter.GjenopptakBehandling
import no.nav.dagpenger.regel.Avklaringspunkter.SøktGjenopptak
import no.nav.dagpenger.regel.OpplysningsTyper.FagsakIdId
import no.nav.dagpenger.regel.prosess.Søknadsprosess
import no.nav.dagpenger.regel.regelsett.vilkår.Gjenopptak
import no.nav.dagpenger.regel.regelsett.vilkår.Meldeplikt.oppfyllerMeldeplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalGjenopptakVurderes
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regelverk.hendelseTypeOpplysningstype
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
    ): StartHendelseResultat {
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

        if (basertPå == null && fagsakId == 0) {
            // Vi tar kun inn søknad så lenge den har en fagsakId i seg. Det vil være søknader om nytt rett (som oppretter sak i Arena).
            // Søknad om gjenopptak vil ikke ha fagsakId.
            // Har vi en behandlingskjede (noe er innvilget) så vil vi også fange opp gjenopptaksøknader.
            return IkkeOpprettet("Hendelse av type $type mangler fagsakId og har ingen behandling å basere seg på")
        }

        val kilde = Systemkilde(meldingsreferanseId, opprettet)
        return Opprettet(
            Behandling(
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
                        if (basertPå == null) {
                            add(Faktum(fagsakIdOpplysningstype, fagsakId, kilde = kilde))
                        }
                    },
                avklaringer =
                    buildList {
                        if (basertPå != null) {
                            add(Avklaring(GjenopptakBehandling))
                        } else {
                            if (søknadstype == Søknadstype.Gjenopptak) {
                                add(Avklaring(SøktGjenopptak))
                            }
                        }
                    },
            ).also {
                it.opplysninger.leggTil(
                    Faktum(søknadIdOpplysningstype, eksternId.id.toString(), kilde = kilde, gyldighetsperiode = Gyldighetsperiode(skjedde)),
                )
                it.opplysninger.leggTil(
                    Faktum(hendelseTypeOpplysningstype, type, gyldighetsperiode = Gyldighetsperiode.kun(skjedde), kilde = kilde),
                )

                if (basertPå != null) {
                    // Om bruker tidligere har hatt minst en periode med rett så begynner med gjenopptaksbehandling som utgangspunkt
                    if (basertPå.vedtakopplysninger.rettighetsperioder.any { rettighetsperiode -> rettighetsperiode.harRett }) {
                        it.opplysninger.leggTil(Faktum(skalGjenopptakVurderes, true, Gyldighetsperiode(skjedde), kilde = kilde))
                    }
                }
            },
        )
    }

    companion object {
        val fagsakIdOpplysningstype = Opplysningstype.heltall(FagsakIdId, "fagsakId")
    }
}
