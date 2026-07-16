package no.nav.dagpenger.regel.prosess

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.BarnetilleggKontroll
import no.nav.dagpenger.regel.regelsett.fastsetting.NyttGrunnbeløpForGrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden.YtelserUtenforFolketrygdenKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.HattLukkedeSakerSiste8UkerKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.MuligGjenopptakKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.TilleggsopplysningsKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.Under18Kontroll
import no.nav.dagpenger.regel.regelsett.vilkår.FulleYtelser.FulleYtelserKontrollpunkt
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt.EØSArbeidKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt.InntektNesteKalendermånedKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt.JobbetUtenforNorgeKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt.ManueltRedigertKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt.PrøverEtterRapporteringsfristKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt.SvangerskapsrelaterteSykepengerKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold.BostedslandKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering.PermitteringKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.PermitteringFraFiskeindustrien.PermitteringFiskKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker.ReellArbeidssøkerKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker.IkkeRegistrertSomArbeidsøkerKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.SkalSamordnes
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.VirkningstidspunktForLangtFremITid
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.TapArbeidstidBeregningsregelKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstidKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt.VernepliktKontroll
import java.time.LocalDate

class Manuellprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(RettighetsperiodePlugin(regelverk))
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val dato = prøvingsdato(opplysninger)
        return Regelkjøring(
            regelverksdato = dato,
            prøvingsperiode = Regelkjøring.Periode(dato),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

    override fun kontrollpunkter() =
        listOf(
            BarnetilleggKontroll,
            EØSArbeidKontroll,
            FulleYtelserKontrollpunkt,
            NyttGrunnbeløpForGrunnlag,
            HattLukkedeSakerSiste8UkerKontroll,
            IkkeRegistrertSomArbeidsøkerKontroll,
            InntektNesteKalendermånedKontroll,
            ManueltRedigertKontroll,
            JobbetUtenforNorgeKontroll,
            MuligGjenopptakKontroll,
            ReellArbeidssøkerKontroll,
            SkalSamordnes,
            SvangerskapsrelaterteSykepengerKontroll,
            VirkningstidspunktForLangtFremITid,
            TapArbeidstidBeregningsregelKontroll,
            beregnetArbeidstidKontroll,
            Under18Kontroll,
            VernepliktKontroll,
            YtelserUtenforFolketrygdenKontroll,
            PrøverEtterRapporteringsfristKontroll,
            PermitteringKontroll,
            TilleggsopplysningsKontroll,
            PermitteringFiskKontroll,
            BostedslandKontroll,
        )

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) =
        opplysninger.kunEgne.somListe().any { it.kilde is Saksbehandlerkilde }

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = prøvingsdato(opplysninger)

    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate = PrøvingsdatoUtleder.utled(opplysninger)
}
