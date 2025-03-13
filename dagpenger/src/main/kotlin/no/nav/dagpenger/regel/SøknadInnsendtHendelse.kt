package no.nav.dagpenger.regel

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.EnHvilkenSomHelstHendelse
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.regel.OpplysningsTyper.FagsakIdId
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

    override val regelverk: Regelverk
        get() = forretningsprosess.regelverk

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring =
        Regelkjøring(prøvingsdato(opplysninger), opplysninger, forretningsprosess)

    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate =
        if (opplysninger.har(Søknadstidspunkt.prøvingsdato)) opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi else skjedde

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        forretningsprosess.ønsketResultat(opplysninger)

    override fun behandling(forrigeBehandling: Behandling?) =
        Behandling(
            behandler =
                EnHvilkenSomHelstHendelse(
                    meldingsreferanseId = meldingsreferanseId,
                    hendelseType = type,
                    ident = ident,
                    eksternId = eksternId,
                    skjedde = skjedde,
                    opprettet = opprettet,
                    forretningsprosess = forretningsprosess,
                ),
            opplysninger =
                listOf(
                    Faktum(fagsakIdOpplysningstype, fagsakId, kilde = Systemkilde(meldingsreferanseId, opprettet)),
                    Faktum(
                        søknadIdOpplysningstype,
                        this.eksternId.id.toString(),
                        kilde = Systemkilde(meldingsreferanseId, opprettet),
                    ),
                    Faktum(
                        hendelseType,
                        type,
                        gyldighetsperiode = Gyldighetsperiode(fom = skjedde),
                        kilde = Systemkilde(meldingsreferanseId, opprettet),
                    ),
                ),
        )

    override fun kontrollpunkter() = forretningsprosess.kontrollpunkter()

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = forretningsprosess.kreverTotrinnskontroll(opplysninger)

    companion object {
        val fagsakIdOpplysningstype = Opplysningstype.heltall(FagsakIdId, "fagsakId")
        val hendelseType = Opplysningstype.tekst(OpplysningsTyper.HendelseTypeId, "hendelseType")
    }
}
