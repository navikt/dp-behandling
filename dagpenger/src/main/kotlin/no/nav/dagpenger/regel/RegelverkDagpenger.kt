package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Regelverk
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
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
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
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.BarnetilleggKontroll
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.NyttGrunnbeløpForGrunnlag
import no.nav.dagpenger.regel.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.fastsetting.PermitteringFraFiskeindustrienFastsetting
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden.YtelserUtenforFolketrygdenKontroll
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting

val RegelverkDagpenger =
    Regelverk(
        KravPåDagpenger.harLøpendeRett,
        Alderskrav.regelsett,
        Beregning.regelsett,
        Dagpengegrunnlag.regelsett,
        DagpengenesStørrelse.regelsett,
        Dagpengeperiode.regelsett,
        Egenandel.regelsett,
        FulleYtelser.regelsett,
        KravPåDagpenger.regelsett,
        Meldeplikt.regelsett,
        Minsteinntekt.regelsett,
        Opphold.regelsett,
        Opptjeningstid.regelsett,
        Permittering.regelsett,
        PermitteringFastsetting.regelsett,
        PermitteringFraFiskeindustrien.regelsett,
        PermitteringFraFiskeindustrienFastsetting.regelsett,
        ReellArbeidssøker.regelsett,
        RegistrertArbeidssøker.regelsett,
        Rettighetstype.regelsett,
        SamordingUtenforFolketrygden.regelsett,
        Samordning.regelsett,
        StreikOgLockout.regelsett,
        Søknadstidspunkt.regelsett,
        TapAvArbeidsinntektOgArbeidstid.regelsett,
        Utdanning.regelsett,
        Utestengning.regelsett,
        Vanligarbeidstid.regelsett,
        Verneplikt.regelsett,
        VernepliktFastsetting.regelsett,
    )

fun oppfyllerKravetTilMinsteinntektEllerVerneplikt(opplysninger: LesbarOpplysninger): Boolean =
    opplysninger.erSann(minsteinntekt) || opplysninger.erSann(oppfyllerKravetTilVerneplikt)

fun kravPåDagpenger(opplysninger: LesbarOpplysninger): Boolean =
    RegelverkDagpenger
        .relevanteVilkår(opplysninger)
        .flatMap { it.betingelser }
        .all { opplysninger.erSann(it) }

val kontrollpunkter =
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
