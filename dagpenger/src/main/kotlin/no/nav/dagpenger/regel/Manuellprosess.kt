package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Klatteland
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Opplysningstype.Companion.periode
import no.nav.dagpenger.opplysning.Regelkjøring
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.RegistrertForretningsprosess
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.opplysning.TidslinjeBygger
import no.nav.dagpenger.opplysning.TidslinjeBygger.Companion.hvorAlleVilkårErOppfylt
import no.nav.dagpenger.regel.Alderskrav.HattLukkedeSakerSiste8UkerKontroll
import no.nav.dagpenger.regel.Alderskrav.MuligGjenopptakKontroll
import no.nav.dagpenger.regel.Alderskrav.TilleggsopplysningsKontroll
import no.nav.dagpenger.regel.Alderskrav.Under18Kontroll
import no.nav.dagpenger.regel.FulleYtelser.FulleYtelserKontrollpunkt
import no.nav.dagpenger.regel.KravPåDagpenger.harLøpendeRett
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
import no.nav.dagpenger.regel.Samordning.SkalSamordnes
import no.nav.dagpenger.regel.Søknadstidspunkt.VirkningstidspunktForLangtFremITid
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.TapArbeidstidBeregningsregelKontroll
import no.nav.dagpenger.regel.TapAvArbeidsinntektOgArbeidstid.beregnetArbeidstidKontroll
import no.nav.dagpenger.regel.Verneplikt.VernepliktKontroll
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.BarnetilleggKontroll
import no.nav.dagpenger.regel.fastsetting.NyttGrunnbeløpForGrunnlag
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.YtelserUtenforFolketrygdenKontroll
import java.time.LocalDate

class Manuellprosess : RegistrertForretningsprosess() {
    override val regelverk = RegelverkDagpenger

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
            EØSArbeidKontroll,
            FulleYtelserKontrollpunkt,
            NyttGrunnbeløpForGrunnlag,
            HattLukkedeSakerSiste8UkerKontroll,
            IkkeRegistrertSomArbeidsøkerKontroll,
            InntektNesteKalendermånedKontroll,
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

    private fun minsteinntekt(opplysninger: LesbarOpplysninger): Boolean = oppfyllerKravetTilMinsteinntektEllerVerneplikt(opplysninger)

    private fun alder(opplysninger: LesbarOpplysninger): Boolean =
        opplysninger.har(Alderskrav.kravTilAlder) &&
            opplysninger.finnOpplysning(Alderskrav.kravTilAlder).verdi

    override fun kreverTotrinnskontroll(opplysninger: LesbarOpplysninger) = minsteinntekt(opplysninger) && alder(opplysninger)

    override fun virkningsdato(opplysninger: LesbarOpplysninger): LocalDate = prøvingsdato(opplysninger)

    override fun regelsett() = regelverk.regelsett

    override fun ønsketResultat(opplysninger: LesbarOpplysninger): List<Opplysningstype<*>> =
        regelverk.regelsett.filter { it.skalKjøres(opplysninger) }.flatMap {
            it.ønsketInformasjon
        }

    override fun klatten(
        tilstand: Klatteland,
        opplysninger: Opplysninger,
    ) {
        when (tilstand) {
            Klatteland.Start -> {}
            Klatteland.Underveis -> {}
            Klatteland.Ferdig -> {
                // Om saksbehandler har pilla, skal vi ikke overstyre med automatikk
                val harPerioder = opplysninger.kunEgne.har(harLøpendeRett)
                val harPilla = harPerioder && opplysninger.kunEgne.finnOpplysning(harLøpendeRett).kilde is Saksbehandlerkilde
                if (harPilla) return

                val vilkår: List<Opplysningstype<Boolean>> =
                    regelverk
                        .regelsettAvType(RegelsettType.Vilkår)
                        .flatMap { it.utfall }

                val utfall =
                    opplysninger
                        .somListe(Egne)
                        .filter { it.opplysningstype in vilkår }
                        .filter { it.erRelevant }
                        .filterIsInstance<Opplysning<Boolean>>()

                return TidslinjeBygger(utfall)
                    .lagPeriode(hvorAlleVilkårErOppfylt())
                    .forEach { periode ->
                        val gyldighetsperiode = Gyldighetsperiode(periode.fraOgMed, periode.tilOgMed)
                        opplysninger.leggTil(Faktum(harLøpendeRett, periode.verdi, gyldighetsperiode))
                    }
            }
        }
    }

    private val opplysningerGyldigPåPrøvingsdato: LesbarOpplysninger.(LocalDate) -> LesbarOpplysninger =
        { forDato(prøvingsdato(this)) }

    private fun prøvingsdato(opplysninger: LesbarOpplysninger): LocalDate =
        opplysninger.kunEgne
            .somListe()
            .last { !it.gyldighetsperiode.fom.isEqual(LocalDate.MIN) }
            .gyldighetsperiode.fom
}
