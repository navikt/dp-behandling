package no.nav.dagpenger.regel
import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
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
import no.nav.dagpenger.regel.regelsett.prosessvilkår.Uriktigeopplysninger
import no.nav.dagpenger.regel.regelsett.vilkår.Alderskrav
import no.nav.dagpenger.regel.regelsett.vilkår.FulleYtelser
import no.nav.dagpenger.regel.regelsett.vilkår.Gjenopptak
import no.nav.dagpenger.regel.regelsett.vilkår.KravPåDagpenger
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
import no.nav.dagpenger.regel.regelsett.vilkår.StreikOgLockout
import no.nav.dagpenger.regel.regelsett.vilkår.Søknad
import no.nav.dagpenger.regel.regelsett.vilkår.Søknadstidspunkt
import no.nav.dagpenger.regel.regelsett.vilkår.TapAvArbeidsinntektOgArbeidstid
import no.nav.dagpenger.regel.regelsett.vilkår.Utdanning
import no.nav.dagpenger.regel.regelsett.vilkår.Utestengning
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt
import no.nav.dagpenger.regel.regelsett.vilkår.Verneplikt.oppfyllerKravetTilVerneplikt

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
    val perioder = dagpengerRettighetsperioder(opplysninger)
    if (perioder.isEmpty()) return Avgjørelse.Uavklart

    val (nye, arvede) = perioder.partition { it.endret }

    return when {
        arvede.isEmpty() -> if (nye.any { it.harRett }) Avgjørelse.Innvilgelse(perioder) else Avgjørelse.Avslag
        nye.isEmpty() && arvede.last().harRett -> Avgjørelse.Endring(perioder)
        nye.isEmpty() && !arvede.last().harRett -> Avgjørelse.Avslag
        arvede.last().harRett && !nye.any { it.harRett } -> Avgjørelse.Stans(perioder)
        !arvede.last().harRett && !nye.any { it.harRett } -> Avgjørelse.Avslag
        !arvede.last().harRett && nye.any { it.harRett } -> Avgjørelse.Gjenopptak(perioder)
        else -> Avgjørelse.Endring(perioder)
    }
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
