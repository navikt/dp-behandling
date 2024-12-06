package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel

val RegelverkDagpenger =
    Regelverk(
        Alderskrav.regelsett,
        Dagpengegrunnlag.regelsett,
        DagpengenesStørrelse.regelsett,
        Dagpengeperiode.regelsett,
        Egenandel.regelsett,
        FulleYtelser.regelsett,
        KravPåDagpenger.regelsett,
        Medlemskap.regelsett,
        Meldeplikt.regelsett,
        Minsteinntekt.regelsett,
        Opphold.regelsett,
        Opptjeningstid.regelsett,
        ReellArbeidssøker.regelsett,
        Rettighetstype.regelsett,
        SamordingUtenforFolketrygden.regelsett,
        Samordning.regelsett,
        StreikOgLockout.regelsett,
        Søknadstidspunkt.regelsett,
        TapAvArbeidsinntektOgArbeidstid.regelsett,
        Utdanning.regelsett,
        Utestengning.regelsett,
        Verneplikt.regelsett,
        Virkningstidspunkt.regelsett,
    )
