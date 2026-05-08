package no.nav.dagpenger.regel

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.opplysning.Forretningsprosess
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.regel.Alderskrav.HattLukkedeSakerSiste8UkerKontroll
import no.nav.dagpenger.regel.Alderskrav.MuligGjenopptakKontroll
import no.nav.dagpenger.regel.Alderskrav.TilleggsopplysningsKontroll
import no.nav.dagpenger.regel.Alderskrav.Under18Kontroll
import no.nav.dagpenger.regel.FulleYtelser.FulleYtelserKontrollpunkt
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
import no.nav.dagpenger.regel.Minsteinntekt.EØSArbeidKontroll
import no.nav.dagpenger.regel.Minsteinntekt.InntektNesteKalendermånedKontroll
import no.nav.dagpenger.regel.Minsteinntekt.JobbetUtenforNorgeKontroll
import no.nav.dagpenger.regel.Minsteinntekt.ManueltRedigertKontroll
import no.nav.dagpenger.regel.Minsteinntekt.PrøverEtterRapporteringsfristKontroll
import no.nav.dagpenger.regel.Minsteinntekt.SvangerskapsrelaterteSykepengerKontroll
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.Opphold.BostedslandKontroll
import no.nav.dagpenger.regel.Permittering.PermitteringKontroll
import no.nav.dagpenger.regel.PermitteringFraFiskeindustrien.PermitteringFiskKontroll
import no.nav.dagpenger.regel.ReellArbeidssøker.ReellArbeidssøkerKontroll
import no.nav.dagpenger.regel.RegistrertArbeidssøker.IkkeRegistrertSomArbeidsøkerKontroll
import no.nav.dagpenger.regel.Rettighetstype.ManglerReellArbeidssøkerKontroll
import no.nav.dagpenger.regel.Samordning.SkalSamordnes
import no.nav.dagpenger.regel.Søknadstidspunkt.SjekkPrøvingsdato
import no.nav.dagpenger.regel.Søknadstidspunkt.VirkningstidspunktForLangtFremITid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.TapArbeidstidBeregningsregelKontroll
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstidKontroll
import no.nav.dagpenger.regel.Verneplikt.VernepliktKontroll
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.BarnetilleggKontroll
import no.nav.dagpenger.regel.fastsetting.NyttGrunnbeløpForGrunnlag
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.YtelserUtenforFolketrygdenKontroll
import java.time.LocalDate

class Søknadsprosess : Forretningsprosess(RegelverkDagpenger) {
    init {
        registrer(RettighetsperiodePlugin(regelverk))
        // Denne flytter prøvinsgdato når rettighetsperiode endres. Det fører til at opptjeningstid og andre tidssensitive behov blir løst på nytt
        registrer(PrøvingsdatoPlugin())
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val egne =
            opplysninger
                .somListe(Egne)
                .filter { !it.gyldighetsperiode.fraOgMed.isEqual(LocalDate.MIN) }
                .filterNot { it.er(harLøpendeRett) }

        // sisteFraOgMed sikrer at regler evalueres på datoen til sist tillagte opplysning
        val sisteFraOgMed = egne.last().gyldighetsperiode.fraOgMed
        val prøvingsdato = PrøvingsdatoUtleder.utled(opplysninger)
        val regelkjøringsdato = maxOf(prøvingsdato, sisteFraOgMed)

        logger.info { "Regelkjøringsdato=$regelkjøringsdato (prøvingsdato=$prøvingsdato, sisteFraOgMed=$sisteFraOgMed)" }

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
            ManueltRedigertKontroll,
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

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate {
        val egne = opplysninger.somListe(Egne)
        val førsteNyeLøpendeRett = egne.firstOrNull { it.er(harLøpendeRett) }?.gyldighetsperiode?.fraOgMed
        return førsteNyeLøpendeRett ?: egne.firstNotNullOf { it.gyldighetsperiode.fraOgMed }
    }

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = minsteinntekt(opplysninger) && alder(opplysninger)

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMap {
            it.ønsketInformasjon
        }

    private fun minsteinntekt(opplysninger: LesbarOpplysninger): Boolean = oppfyllerKravetTilMinsteinntektEllerVerneplikt(opplysninger)

    private fun alder(opplysninger: LesbarOpplysninger): Boolean =
        opplysninger.har(Alderskrav.kravTilAlder) &&
            opplysninger.finnOpplysning(Alderskrav.kravTilAlder).verdi

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
