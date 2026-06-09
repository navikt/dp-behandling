package no.nav.dagpenger.regel.prosess

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.RegelverkDagpenger
import no.nav.dagpenger.regel.oppfyllerKravetTilMinsteinntektEllerVerneplikt
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.BarnetilleggKontroll
import no.nav.dagpenger.regel.regelsett.fastsetting.NyttGrunnbeløpForGrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden.YtelserUtenforFolketrygdenKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.HattLukkedeSakerSiste8UkerKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.MuligGjenopptakKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.TilleggsopplysningsKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav.Under18Kontroll
import no.nav.dagpenger.regel.regelsett.vilkår.FulleYtelser.FulleYtelserKontrollpunkt
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger.harLøpendeRett
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
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.ManglerReellArbeidssøkerKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning.SkalSamordnes
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.SjekkPrøvingsdato
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt.VirkningstidspunktForLangtFremITid
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.TapArbeidstidBeregningsregelKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstidKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Utestengning.utestengtKontroll
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt.VernepliktKontroll
import java.time.LocalDate

class Søknadsprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(RettighetsperiodePlugin(regelverk))
        // Denne flytter prøvinsgdato når rettighetsperiode endres. Det fører til at opptjeningstid og andre tidssensitive behov blir løst på nytt
        registrer(PrøvingsdatoPlugin())
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val prøvingsdato = PrøvingsdatoUtleder.utled(opplysninger)
        val ubehandlede = opplysninger.ubehandledeDatoer()

        val regelkjøringsdato =
            if (ubehandlede.isNotEmpty()) {
                maxOf(prøvingsdato, ubehandlede.first())
            } else {
                prøvingsdato
            }

        logger.info { "Regelkjøringsdato=$regelkjøringsdato (prøvingsdato=$prøvingsdato, ubehandlede=$ubehandlede)" }

        return Regelkjøring(
            regelverksdato = virkningsdato(opplysninger),
            prøvingsperiode = Regelkjøring.Enkeltdager(regelkjøringsdato),
            opplysninger = opplysninger,
            forretningsprosess = this,
        )
    }

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
            ManueltRedigertKontroll,
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
            utestengtKontroll,
        )

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
        val egne = opplysninger.somListe(Egne)
        val førsteNyeLøpendeRett = egne.firstOrNull { it.er(harLøpendeRett) }?.gyldighetsperiode?.fraOgMed
        return førsteNyeLøpendeRett ?: egne.firstNotNullOf { it.gyldighetsperiode.fraOgMed }
    }

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = minsteinntekt(opplysninger) && alder(opplysninger)

    private fun minsteinntekt(opplysninger: LesbarOpplysninger): Boolean = oppfyllerKravetTilMinsteinntektEllerVerneplikt(opplysninger)

    private fun alder(opplysninger: LesbarOpplysninger): Boolean =
        opplysninger.har(Alderskrav.kravTilAlder) &&
            opplysninger.finnOpplysning(Alderskrav.kravTilAlder).verdi

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
