package no.nav.dagpenger.regel

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.hendelser.StartHendelse
import no.nav.dagpenger.behandling.modell.hendelser.SøknadId
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Systemkilde
import no.nav.dagpenger.regel.Alderskrav.HattLukkedeSakerSiste8UkerKontroll
import no.nav.dagpenger.regel.Alderskrav.MuligGjenopptakKontroll
import no.nav.dagpenger.regel.Alderskrav.Under18Kontroll
import no.nav.dagpenger.regel.FulleYtelser.FulleYtelserKontrollpunkt
import no.nav.dagpenger.regel.KravPåDagpenger.VirkningstidspunktForLangtFramITid
import no.nav.dagpenger.regel.KravPåDagpenger.minsteinntektEllerVerneplikt
import no.nav.dagpenger.regel.Minsteinntekt.EØSArbeidKontroll
import no.nav.dagpenger.regel.Minsteinntekt.InntektNesteKalendermånedKontroll
import no.nav.dagpenger.regel.Minsteinntekt.JobbetUtenforNorgeKontroll
import no.nav.dagpenger.regel.Minsteinntekt.SvangerskapsrelaterteSykepengerKontroll
import no.nav.dagpenger.regel.Minsteinntekt.ØnskerEtterRapporteringsfristKontroll
import no.nav.dagpenger.regel.OpplysningsTyper.FagsakIdId
import no.nav.dagpenger.regel.Permittering.PermitteringKontroll
import no.nav.dagpenger.regel.ReellArbeidssøker.IkkeRegistrertSomArbeidsøkerKontroll
import no.nav.dagpenger.regel.ReellArbeidssøker.ReellArbeidssøkerKontroll
import no.nav.dagpenger.regel.SamordingUtenforFolketrygden.YtelserUtenforFolketrygdenKontroll
import no.nav.dagpenger.regel.Samordning.SkalSamordnes
import no.nav.dagpenger.regel.Søknadstidspunkt.SøknadstidspunktForLangtFramITid
import no.nav.dagpenger.regel.Søknadstidspunkt.søknadIdOpplysningstype
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.TapArbeidstidBeregningsregelKontroll
import no.nav.dagpenger.regel.Verneplikt.VernepliktKontroll
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.BarnetilleggKontroll
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SøknadInnsendtHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    søknadId: UUID,
    gjelderDato: LocalDate,
    fagsakId: Int,
    opprettet: LocalDateTime,
) : StartHendelse(meldingsreferanseId, ident, SøknadId(søknadId), gjelderDato, fagsakId, opprettet) {
    override val forretningsprosess = Søknadsprosess()

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring =
        Regelkjøring(prøvingsdato(opplysninger), opplysninger, forretningsprosess)

    override fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate =
        if (opplysninger.har(Søknadstidspunkt.prøvingsdato)) opplysninger.finnOpplysning(Søknadstidspunkt.prøvingsdato).verdi else skjedde

    private fun kravPåDagpenger(opplysninger: LesbarOpplysninger): Boolean =
        opplysninger.har(KravPåDagpenger.kravPåDagpenger) &&
            opplysninger.finnOpplysning(KravPåDagpenger.kravPåDagpenger).verdi

    private fun minsteinntekt(opplysninger: LesbarOpplysninger): Boolean =
        opplysninger.har(minsteinntektEllerVerneplikt) &&
            opplysninger.finnOpplysning(minsteinntektEllerVerneplikt).verdi

    private fun alder(opplysninger: LesbarOpplysninger): Boolean =
        opplysninger.har(Alderskrav.kravTilAlder) &&
            opplysninger.finnOpplysning(Alderskrav.kravTilAlder).verdi

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        kravPåDagpenger(opplysninger) || (minsteinntekt(opplysninger) && alder(opplysninger))

    override fun behandling() =
        Behandling(
            behandler = this,
            opplysninger =
                listOf(
                    Faktum(fagsakIdOpplysningstype, fagsakId, kilde = Systemkilde(meldingsreferanseId, opprettet)),
                    Faktum(
                        søknadIdOpplysningstype,
                        this.eksternId.id.toString(),
                        kilde = Systemkilde(meldingsreferanseId, opprettet),
                    ),
                ),
        )

    override fun kontrollpunkter() =
        listOf(
            BarnetilleggKontroll,
            EØSArbeidKontroll,
            FulleYtelserKontrollpunkt,
            HattLukkedeSakerSiste8UkerKontroll,
            IkkeRegistrertSomArbeidsøkerKontroll,
            InntektNesteKalendermånedKontroll,
            JobbetUtenforNorgeKontroll,
            MuligGjenopptakKontroll,
            ReellArbeidssøkerKontroll,
            SkalSamordnes,
            SvangerskapsrelaterteSykepengerKontroll,
            SøknadstidspunktForLangtFramITid,
            TapArbeidstidBeregningsregelKontroll,
            Under18Kontroll,
            VernepliktKontroll,
            VirkningstidspunktForLangtFramITid,
            YtelserUtenforFolketrygdenKontroll,
            ØnskerEtterRapporteringsfristKontroll,
            PermitteringKontroll,
        )

    companion object {
        val fagsakIdOpplysningstype = Opplysningstype.heltall(FagsakIdId, "fagsakId")
    }
}
