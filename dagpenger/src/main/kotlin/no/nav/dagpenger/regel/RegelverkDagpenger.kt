package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.RettighetsperiodeStrategi
import no.nav.dagpenger.opplysning.Utbetaling
import no.nav.dagpenger.opplysning.UtbetalingerStrategi
import no.nav.dagpenger.regel.Minsteinntekt.minsteinntekt
import no.nav.dagpenger.regel.Verneplikt.oppfyllerKravetTilVerneplikt
import no.nav.dagpenger.regel.beregning.Beregning
import no.nav.dagpenger.regel.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
import no.nav.dagpenger.regel.fastsetting.Dagpengeperiode
import no.nav.dagpenger.regel.fastsetting.Egenandel
import no.nav.dagpenger.regel.fastsetting.PermitteringFastsetting
import no.nav.dagpenger.regel.fastsetting.PermitteringFraFiskeindustrienFastsetting
import no.nav.dagpenger.regel.fastsetting.SamordingUtenforFolketrygden
import no.nav.dagpenger.regel.fastsetting.Vanligarbeidstid
import no.nav.dagpenger.regel.fastsetting.VernepliktFastsetting
import no.nav.dagpenger.regel.prosessvilkår.OmgjøringUtenKlage
import no.nav.dagpenger.regel.prosessvilkår.Uriktigeopplysninger

val RegelverkDagpenger =
    Regelverk(
        Alderskrav.regelsett,
        Beregning.regelsett,
        Dagpengegrunnlag.regelsett,
        DagpengenesStørrelse.regelsett,
        Dagpengeperiode.regelsett,
        Egenandel.regelsett,
        FulleYtelser.regelsett,
        Gjenopptak.regelsett,
        KravPåDagpenger.regelsett,
        Meldeplikt.regelsett,
        Minsteinntekt.regelsett,
        Opphold.regelsett,
        Opptjeningstid.regelsett,
        OmgjøringUtenKlage.regelsett,
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
        Søknad.regelsett,
        Søknadstidspunkt.regelsett,
        TapAvArbeidsinntektOgArbeidstid.regelsett,
        Uriktigeopplysninger.regelsett,
        Utdanning.regelsett,
        Utestengning.regelsett,
        Vanligarbeidstid.regelsett,
        Verneplikt.regelsett,
        VernepliktFastsetting.regelsett,
        rettighetsperiodeStrategi = DagpengerRettighetsperiodeStrategi(),
        utbetalingerStrategi = DagpengerUtbetalingStrategi(),
    )

fun oppfyllerKravetTilMinsteinntektEllerVerneplikt(opplysninger: LesbarOpplysninger): Boolean =
    opplysninger.erSann(minsteinntekt) || opplysninger.erSann(oppfyllerKravetTilVerneplikt)

fun kravPåDagpenger(opplysninger: LesbarOpplysninger): Boolean =
    RegelverkDagpenger
        .relevanteVilkår(opplysninger)
        .flatMap { it.betingelser }
        .all { opplysninger.erSann(it) }

class DagpengerRettighetsperiodeStrategi : RettighetsperiodeStrategi {
    override fun rettighetsperioder(opplysninger: LesbarOpplysninger): List<Rettighetsperiode> {
        val egne = opplysninger.somListe(Egne)
        return opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett).map { periode ->
            Rettighetsperiode(
                fraOgMed = periode.gyldighetsperiode.fraOgMed,
                tilOgMed = periode.gyldighetsperiode.tilOgMed,
                harRett = periode.verdi,
                endret = egne.contains(periode),
            )
        }
    }
}

class DagpengerUtbetalingStrategi : UtbetalingerStrategi {
    override fun utbetalinger(opplysninger: LesbarOpplysninger): List<Utbetaling> {
        val meldeperioder = opplysninger.finnAlle(Beregning.meldeperiode)

        val egneId = opplysninger.somListe(Egne).map { it.id }
        val løpendeRett = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
        val satser = opplysninger.finnAlle(dagsatsEtterSamordningMedBarnetillegg)
        val dager = opplysninger.finnAlle(Beregning.utbetaling).associateBy { it.gyldighetsperiode.fraOgMed }

        return meldeperioder.flatMap { periode ->
            periode.verdi.mapNotNull { dato ->
                if (løpendeRett.filter { it.verdi }.none { it.gyldighetsperiode.inneholder(dato) }) {
                    // Har ikke løpende rett i denne perioden, så ingen utbetaling
                    return@mapNotNull null
                }

                val dag = dager[dato] ?: throw IllegalStateException("Mangler utbetaling for dag $dato")
                val sats = satser.first { it.gyldighetsperiode.inneholder(dato) }.verdi
                Utbetaling.Meldekort(
                    meldeperiode = periode.verdi.hashCode().toString(),
                    dato = dato,
                    sats = sats.verdien.toInt(),
                    utbetaling = dag.verdi.heleKroner.toInt(),
                    endret = (dag.id in egneId),
                )
            }
        }
    }
}
