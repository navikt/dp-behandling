package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Alderskrav.HattLukkedeSakerSiste8UkerKontroll
import no.nav.dagpenger.regel.Alderskrav.MuligGjenopptakKontroll
import no.nav.dagpenger.regel.Alderskrav.TilleggsopplysningsKontroll
import no.nav.dagpenger.regel.Alderskrav.Under18Kontroll
import no.nav.dagpenger.regel.FulleYtelser.FulleYtelserKontrollpunkt
import no.nav.dagpenger.regel.Minsteinntekt.EØSArbeidKontroll
import no.nav.dagpenger.regel.Minsteinntekt.InntektNesteKalendermånedKontroll
import no.nav.dagpenger.regel.Minsteinntekt.JobbetUtenforNorgeKontroll
import no.nav.dagpenger.regel.Minsteinntekt.PrøverEtterRapporteringsfristKontroll
import no.nav.dagpenger.regel.Minsteinntekt.SvangerskapsrelaterteSykepengerKontroll
import no.nav.dagpenger.regel.Opphold.BostedslandKontroll
import no.nav.dagpenger.regel.Permittering.PermitteringKontroll
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.PermitteringFiskKontroll
import no.nav.dagpenger.regel.ReellArbeidssøker.ReellArbeidssøkerKontroll
import no.nav.dagpenger.regel.RegistrertArbeidssøker.IkkeRegistrertSomArbeidsøkerKontroll
import no.nav.dagpenger.regel.Rettighetstype.ManglerReellArbeidssøkerKontroll
import no.nav.dagpenger.regel.Samordning.SkalSamordnes
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.TapArbeidstidBeregningsregelKontroll
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstidKontroll
import no.nav.dagpenger.regel.Verneplikt.VernepliktKontroll
import no.nav.dagpenger.regel.Virkningstidspunkt.SjekkPrøvingsdato
import no.nav.dagpenger.regel.Virkningstidspunkt.VirkningstidspunktForLangtFremITid
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.BarnetilleggKontroll
import no.nav.dagpenger.regel.fastsetting.NyttGrunnbeløpForGrunnlag
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.YtelserUtenforFolketrygdenKontroll
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse.Companion.hendelseTypeOpplysningstype
import java.time.LocalDate

class Søknadsprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(RettighetsperiodePlugin(regelverk))
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring =
        Regelkjøring(
            virkningsdato(opplysninger),
            opplysninger,
            this,
            opplysningerGyldigPåPrøvingsdato,
        )

    override fun kontrollpunkter() =
        listOf(
            BarnetilleggKontroll,
            BostedslandKontroll,
            EØSArbeidKontroll,
            FulleYtelserKontrollpunkt,
            HattLukkedeSakerSiste8UkerKontroll,
            IkkeRegistrertSomArbeidsøkerKontroll,
            InntektNesteKalendermånedKontroll,
            JobbetUtenforNorgeKontroll,
            ManglerReellArbeidssøkerKontroll,
            MuligGjenopptakKontroll,
            NyttGrunnbeløpForGrunnlag,
            PermitteringFiskKontroll,
            PermitteringKontroll,
            PrøverEtterRapporteringsfristKontroll,
            ReellArbeidssøkerKontroll,
            SjekkPrøvingsdato,
            SkalSamordnes,
            SvangerskapsrelaterteSykepengerKontroll,
            TapArbeidstidBeregningsregelKontroll,
            TilleggsopplysningsKontroll,
            Under18Kontroll,
            VernepliktKontroll,
            VirkningstidspunktForLangtFremITid,
            YtelserUtenforFolketrygdenKontroll,
            beregnetArbeidstidKontroll,
        )

    private fun minsteinntekt(opplysninger: LesbarOpplysninger): Boolean = oppfyllerKravetTilMinsteinntektEllerVerneplikt(opplysninger)

    private fun alder(opplysninger: LesbarOpplysninger): Boolean =
        opplysninger.har(Alderskrav.kravTilAlder) &&
            opplysninger.finnOpplysning(Alderskrav.kravTilAlder).verdi

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = minsteinntekt(opplysninger) && alder(opplysninger)

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = prøvingsdato(opplysninger)

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMap {
            it.ønsketInformasjon
        }

    private val opplysningerGyldigPåPrøvingsdato: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger =
        { forDato(prøvingsdato(this)) }

    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate =
        if (opplysninger.har(Virkningstidspunkt.prøvingsdato)) {
            opplysninger.finnOpplysning(Virkningstidspunkt.prøvingsdato).verdi
        } else if (opplysninger.har(hendelseTypeOpplysningstype)) {
            opplysninger.finnOpplysning(hendelseTypeOpplysningstype).gyldighetsperiode.fraOgMed
        } else {
            throw IllegalStateException("Mangler både prøvingsdato og hendelsedato. Må ha en dato å ta utgangspunkt i for behandlingen.")
        }
}
