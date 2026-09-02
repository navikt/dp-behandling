package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.regelsett.fastsetting.Egenandel
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.regelsett.fastsetting.PermitteringFraFiskeindustrienFastsetting
import no.nav.dagpenger.regel.regelsett.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.regelsett.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.regelsett.fastsetting.VernepliktFastsetting
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlage
import no.nav.dagpenger.regel.regelsett.prosessvilkår.OmgjøringUtenKlageValg
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.Eksport
import no.nav.dagpenger.regel.regelsett.vilkår.FulleYtelser
import no.nav.dagpenger.regel.regelsett.vilkår.Gjenopptak
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
import no.nav.dagpenger.regel.regelsett.vilkår.MedlemmetOpplysningsplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Meldeplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt
import no.nav.dagpenger.regel.regelsett.vilkår.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.regelsett.vilkår.Opphold
import no.nav.dagpenger.regel.regelsett.vilkår.Opptjeningstid
import no.nav.dagpenger.regel.regelsett.vilkår.Permittering
import no.nav.dagpenger.regel.regelsett.vilkår.PermitteringFraFiskeindustrien
import no.nav.dagpenger.regel.regelsett.vilkår.ReellArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.RegistrertArbeidssøker
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype
import no.nav.dagpenger.regel.regelsett.vilkår.Samordning
import no.nav.dagpenger.regel.regelsett.vilkår.Sanksjonsperiode
import no.nav.dagpenger.regel.regelsett.vilkår.StreikOgLockout
import no.nav.dagpenger.regel.regelsett.vilkår.Søknad
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.TidsbegrensetBortfall
import no.nav.dagpenger.regel.regelsett.vilkår.TreMeldePerioderUtentilstrekkeligTapAvArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Uriktigeopplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utestengning
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.regelverk.Avgjørelseberegning
import no.nav.dagpenger.regel.regelverk.Rettighetsperioder
import no.nav.dagpenger.regel.regelverk.Utbetalinger

val RegelverkDagpenger =
    Regelverk(
        navn = RegelverkType("Dagpenger"),
        rettighetsperiodeberegning = Rettighetsperioder,
        utbetalingsberegning = Utbetalinger,
        avgjørelsesberegning = Avgjørelseberegning,
        Alderskrav.regelsett,
        Beregning.regelsett,
        Dagpengegrunnlag.regelsett,
        DagpengenesStørrelse.regelsett,
        Dagpengeperiode.regelsett,
        Egenandel.regelsett,
        Eksport.regelsett,
        FulleYtelser.regelsett,
        Gjenopptak.regelsett,
        KravPåDagpenger.regelsett,
        MedlemmetOpplysningsplikt.regelsett,
        Meldeplikt.regelsett,
        Minsteinntekt.regelsett,
        OmgjøringUtenKlageValg.regelsett,
        OmgjøringUtenKlage.regelsett,
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
        Sanksjonsperiode.regelsett,
        StreikOgLockout.regelsett,
        Søknad.regelsett,
        Søknadstidspunkt.regelsett,
        TapAvArbeidsinntektOgArbeidstid.regelsett,
        TidsbegrensetBortfall.regelsett,
        TreMeldePerioderUtentilstrekkeligTapAvArbeidstid.regelsett,
        Uriktigeopplysninger.regelsett,
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
        .asSequence()
        .flatMap { it.betingelser.asSequence() }
        .all { opplysninger.erSann(it) }
