package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.Utbetaling
import no.nav.dagpenger.opplysning.Ytelsestype
import no.nav.dagpenger.regel.regelsett.beregning.Beregning
import no.nav.dagpenger.regel.regelsett.fastsetting.Dagpengegrunnlag
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse
import no.nav.dagpenger.regel.regelsett.fastsetting.DagpengenesStørrelse.dagsatsEtterSamordningMedBarnetillegg
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
import no.nav.dagpenger.regel.regelsett.vilkår.Rettighetstype.skalGjenopptakVurderes
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
import java.time.temporal.ChronoUnit

val RegelverkDagpenger =
    Regelverk(
        navn = RegelverkType("Dagpenger"),
        rettighetsperiodeberegning = ::dagpengerRettighetsperioder,
        utbetalingsberegning = ::dagpengerUtbetalinger,
        avgjørelsesberegning = ::dagpengerAvgjørelse,
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

private fun dagpengerRettighetsperioder(opplysninger: LesbarOpplysninger): List<Rettighetsperiode> {
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

private fun dagpengerAvgjørelse(opplysninger: LesbarOpplysninger): Avgjørelse {
    // Perioder kan komme i vilkårlig rekkefølge (f.eks. en etterregistrert/tilbakedatert periode som
    // dukker opp etter en periode som allerede er kjent) - sorter derfor kronologisk før vi resonnerer
    // om hva som faktisk er den gjeldende (siste) statusen.
    val rettighetsopplysninger = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
    val perioder = dagpengerRettighetsperioder(opplysninger).sortedBy { it.fraOgMed }
    if (perioder.isEmpty()) return Avgjørelse.Uavklart

    val (nye, arvede) = perioder.partition { it.endret }

    // Ingen nye perioder betyr at forrige avgjørelse videreføres uendret, f.eks. ved meldekort
    if (nye.isEmpty()) return Avgjørelse.Endring

    val forrigePeriode = arvede.lastOrNull()

    // Aller første rettighetsperiode som er vurdert - ingenting å sammenligne med. Avgjørelsen reflekterer
    // om denne (første) behandlingen i det hele tatt innvilger rett, selv om en senere periode i samme
    // sending går tilbake til ingen rett (f.eks. innvilget med en innebygd fremtidig stans).
    if (forrigePeriode == null) {
        return if (nye.any { it.harRett }) Avgjørelse.Innvilgelse else Avgjørelse.Avslag
    }

    // Den kronologisk siste perioden er den reelle, gjeldende statusen - uavhengig av om den er ny eller arvet
    val gjeldendePeriode = perioder.last()

    // Hvis den gjeldende perioden er arvet (ikke ny), er de nye periodene i realiteten tilbakedaterte og
    // fullstendig overstyrt av en allerede kjent, senere periode som ikke er endret nå. Da har den gjeldende
    // statusen ikke endret seg, uansett hva de nye periodene selv sier - altså bare en Endring.
    if (!gjeldendePeriode.endret) return Avgjørelse.Endring

    return when {
        // Hadde rett fra før, men ender nå uten rett
        forrigePeriode.harRett && !gjeldendePeriode.harRett -> Avgjørelse.Stans

        // Hadde rett fra før, og har fortsatt rett - men med et opphold mellom periodene
        forrigePeriode.harRett && harGapEtterForrigePeriode(perioder, forrigePeriode, rettighetsopplysninger) -> Avgjørelse.Gjenopptak

        // Hadde rett fra før, og har fortsatt rett uten opphold
        forrigePeriode.harRett -> Avgjørelse.Endring

        // Hadde ikke rett fra før, men får ny rett
        gjeldendePeriode.harRett -> Avgjørelse.Gjenopptak

        // Hadde ikke rett fra før, og har fortsatt ikke rett
        opplysninger.kunEgne.har(skalGjenopptakVurderes) -> Avgjørelse.Avslag
        else -> Avgjørelse.Stans
    }
}

private fun harGapEtterForrigePeriode(
    perioder: List<Rettighetsperiode>,
    forrigePeriode: Rettighetsperiode,
    rettighetsopplysninger: List<Opplysning<Boolean>>,
): Boolean {
    val nestePeriode = perioder.getOrNull(perioder.indexOf(forrigePeriode) + 1) ?: return false

    val dagerMellom = ChronoUnit.DAYS.between(forrigePeriode.tilOgMed, nestePeriode.fraOgMed)

    // Mer enn ett dags avstand er alltid et reelt opphold.
    if (dagerMellom > 1) return true
    if (dagerMellom < 1) return false

    // Ett dags avstand er tvetydig: perioder som opprinnelig hadde et opphold (f.eks. stans etterfulgt av
    // gjenopptak samme dag) kan bli slått sammen slik at det ser ut som periodene ligger rett inntil
    // hverandre (kant-i-kant), selv om det egentlig var en reell stans imellom. Vi skiller de to tilfellene
    // ved å sjekke om den nye perioden faktisk erstatter en tidligere vurdert periode (reelt opphold), eller
    // om den bare er en ren videreføring inn i tidligere uvurdert tid (ikke noe opphold).
    val nesteOpplysning = rettighetsopplysninger.firstOrNull { it.gyldighetsperiode.fraOgMed == nestePeriode.fraOgMed }
    return nesteOpplysning?.erstatter != null
}

private fun dagpengerUtbetalinger(opplysninger: LesbarOpplysninger): List<Utbetaling> {
    val meldeperioder = opplysninger.finnAlle(Beregning.meldeperiode)

    val egneId = opplysninger.somListe(Egne).map { it.id }
    val løpendeRett = opplysninger.finnAlle(KravPåDagpenger.harLøpendeRett)
    val satser = opplysninger.finnAlle(dagsatsEtterSamordningMedBarnetillegg)
    val dager = opplysninger.finnAlle(Beregning.utbetaling).associateBy { it.gyldighetsperiode.fraOgMed }

    return meldeperioder.flatMap { periode ->
        periode.verdi.mapNotNull { dato ->
            if (løpendeRett.filter { it.verdi }.none { it.gyldighetsperiode.inneholder(dato) }) {
                return@mapNotNull null
            }

            val dag = dager[dato] ?: throw IllegalStateException("Mangler utbetaling for dag $dato")
            val sats = satser.first { it.gyldighetsperiode.inneholder(dato) }.verdi
            Utbetaling(
                meldeperiode = periode.verdi.hashCode().toString(),
                dato = dato,
                sats = sats.verdien.toInt(),
                utbetaling = dag.verdi.heleKroner.toInt(),
                endret = (dag.id in egneId),
                ytelsestype = Ytelsestype("Ordinær"),
            )
        }
    }
}
