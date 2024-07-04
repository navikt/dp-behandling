package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.Alderskrav.AvslagAlder
import no.nav.dagpenger.regel.KravPåDagpenger
import no.nav.dagpenger.regel.KravPåDagpenger.Innvilgelse
import no.nav.dagpenger.regel.Minsteinntekt.AvslagInntekt
import no.nav.dagpenger.regel.Minsteinntekt.EØSArbeidKontroll
import no.nav.dagpenger.regel.Minsteinntekt.InntektNesteKalendermånedKontroll
import no.nav.dagpenger.regel.Minsteinntekt.JobbetUtenforNorgeKontroll
import no.nav.dagpenger.regel.Minsteinntekt.SvangerskapsrelaterteSykepengerKontroll
import no.nav.dagpenger.regel.SøknadInnsendtRegelsett
import no.nav.dagpenger.regel.Søknadstidspunkt.HattLukkedeSakerSiste8UkerKontroll
import no.nav.dagpenger.regel.Søknadstidspunkt.MuligGjenopptakKontroll
import no.nav.dagpenger.regel.Verneplikt.VernepliktKontroll
import java.time.LocalDate
import java.util.UUID

class SøknadInnsendtHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    søknadId: UUID,
    gjelderDato: LocalDate,
    fagsakId: Int,
) : StartHendelse(meldingsreferanseId, ident, SøknadId(søknadId), gjelderDato, fagsakId) {
    override fun regelsett() = SøknadInnsendtRegelsett.regelsett

    override fun avklarer(): Opplysningstype<Boolean> = KravPåDagpenger.kravPåDagpenger

    private companion object {
        val fagsakIdOpplysningstype = Opplysningstype.somHeltall("fagsakId")
    }

    override fun behandling() =
        Behandling(
            this,
            listOf(
                Faktum(fagsakIdOpplysningstype, fagsakId),
            ),
        )

    override fun konklusjonStrategier() = listOf(AvslagAlder, AvslagInntekt, Innvilgelse)

    override fun kontrollpunkter() =
        listOf(
            VernepliktKontroll,
            SvangerskapsrelaterteSykepengerKontroll,
            EØSArbeidKontroll,
            JobbetUtenforNorgeKontroll,
            InntektNesteKalendermånedKontroll,
            HattLukkedeSakerSiste8UkerKontroll,
            MuligGjenopptakKontroll,
        )
}
