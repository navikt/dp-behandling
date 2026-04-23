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
import no.nav.dagpenger.regel.Minsteinntekt.EØSArbeidKontroll
import no.nav.dagpenger.regel.Minsteinntekt.InntektNesteKalendermånedKontroll
import no.nav.dagpenger.regel.Minsteinntekt.JobbetUtenforNorgeKontroll
import no.nav.dagpenger.regel.Minsteinntekt.ManueltRedigertKontroll
import no.nav.dagpenger.regel.Minsteinntekt.PrøverEtterRapporteringsfristKontroll
import no.nav.dagpenger.regel.Minsteinntekt.SvangerskapsrelaterteSykepengerKontroll
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
        registrer(PrøvingsdatoPlugin())
    }

    override fun regelkjøring(opplysninger: Opplysninger): Regelkjøring {
        val egne = opplysninger.somListe(Egne).filter { !it.gyldighetsperiode.fraOgMed.isEqual(LocalDate.MIN) }

        // Første mulige dato vi kan vurdere fra
        val førsteGrensedato = egne.minOf { it.gyldighetsperiode.fraOgMed }

        // Prøvingsdato brukes til å forskyve innvilgelse fram i tid
        val førsteØnsketVurderingsdato = egne.firstOrNull { it.er(Søknadstidspunkt.prøvingsdato) }?.verdi as LocalDate?
        val førsteVurderingsdato = listOfNotNull(førsteØnsketVurderingsdato, førsteGrensedato).max()

        // Vurder hele perioden, ikke bare en enkelt dato
        val siste = egne.maxOf { it.gyldighetsperiode.fraOgMed }

        logger.info { "FØRSTE OG SISTE = $førsteGrensedato, $førsteVurderingsdato, $siste" }

        return Regelkjøring(
            virkningsdato(opplysninger),
            prøvingsperiode = Regelkjøring.Periode(start = førsteVurderingsdato, endInclusive = siste),
            opplysninger,
            this,
            // opplysningerGyldigPåPrøvingsdato,
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

    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate {
        val søknadsdato =
            opplysninger.kunEgne
                .finnNullableOpplysning(Søknadstidspunkt.søknadIdOpplysningstype)
                ?.gyldighetsperiode
                ?.fraOgMed

        val sisteFraOgMed =
            opplysninger.kunEgne
                .somListe()
                .last { !it.gyldighetsperiode.fraOgMed.isEqual(LocalDate.MIN) }
                .gyldighetsperiode.fraOgMed

        if (søknadsdato == null) {
            return sisteFraOgMed
        }

        return maxOf(søknadsdato, sisteFraOgMed)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
/*
første mulige vurderingsdato

1. Vi må begynne et sted. Vi trenger en dato å begynne på. Den setter grense for hvor langt bak i tid man kan vilkårsprøve
2. Behov må ha en dato de spør på - mulig løsning å spørre fraOgMed søknadsdato 👆
3. Dato for innvilgelse må kunne flyttes fram i tid fra første mulige vurderingsdato
4. Inntekt må kunne hentes fram i tid. Henger sammen med 👆
5. Vi kan lage en knapp som kjører alt på nytt
 */
