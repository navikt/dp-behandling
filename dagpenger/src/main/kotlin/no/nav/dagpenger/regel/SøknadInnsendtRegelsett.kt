package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode

val RegelverkDagpenger =
    Regelverk(
        Alderskrav.regelsett,
        Dagpengeperiode.regelsett,
        KravPåDagpenger.regelsett,
        Medlemskap.regelsett,
        Meldeplikt.regelsett,
        Minsteinntekt.regelsett,
        Opptjeningstid.regelsett,
        ReellArbeidssøker.regelsett,
        Rettighetstype.regelsett,
        StreikOgLockout.regelsett,
        Søknadstidspunkt.regelsett,
        Utdanning.regelsett,
        Utestengning.regelsett,
        Verneplikt.regelsett,
        Virkningstidspunkt.regelsett,
    )
