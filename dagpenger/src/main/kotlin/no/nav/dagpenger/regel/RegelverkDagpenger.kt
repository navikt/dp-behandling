package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.RegelsettType
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.regel.KravPåDagpenger.minsteinntektEllerVerneplikt
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting

val RegelverkDagpenger =
    Regelverk(
        Alderskrav.regelsett,
        Vanligarbeidstid.regelsett,
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
        VernepliktFastsetting.regelsett,
    )

fun kravetTilAlderOgMinsteinntektErOppfylt(opplysninger: LesbarOpplysninger): Boolean = opplysninger.erSann(minsteinntektEllerVerneplikt)

fun kravPåDagpenger(opplysninger: LesbarOpplysninger): Boolean =
    RegelverkDagpenger.regelsett
        .filter { it.type == RegelsettType.Vilkår }
        .filter { it.erRelevant(opplysninger) }
        .mapNotNull { it.utfall }
        .all { opplysninger.erSann(it) }
// opplysninger.erSann(kravPåDagpenger)
