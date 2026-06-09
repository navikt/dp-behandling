package no.nav.dagpenger.ferietillegg

import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.Utbetaling
import no.nav.dagpenger.opplysning.Ytelsestype
import java.time.LocalDate

val RegelverkFerietillegg =
    Regelverk(
        navn = RegelverkType("Ferietillegg"),
        rettighetsperiodeberegning = ::ferietilleggRettighetsperioder,
        utbetalingsberegning = ::ferietilleggUtbetalinger,
        avgjørelsesberegning = ::ferietilleggAvgjørelse,
        KravPåFerietillegg.regelsett,
        FerietilleggBeløp.regelsett,
    )

private fun ferietilleggRettighetsperioder(opplysninger: LesbarOpplysninger): List<Rettighetsperiode> {
    val egne = opplysninger.somListe(Egne)
    return opplysninger.finnAlle(KravPåFerietillegg.harKravpåFerietillegg).map { periode ->
        Rettighetsperiode(
            fraOgMed = periode.gyldighetsperiode.fraOgMed,
            tilOgMed = periode.gyldighetsperiode.tilOgMed,
            harRett = periode.verdi,
            endret = egne.contains(periode),
        )
    }
}

private fun ferietilleggAvgjørelse(opplysninger: LesbarOpplysninger): Avgjørelse {
    val perioder = ferietilleggRettighetsperioder(opplysninger)
    if (perioder.isEmpty()) return Avgjørelse.Uavklart

    val (nye, arvede) = perioder.partition { it.endret }

    return when {
        arvede.isEmpty() -> if (nye.any { it.harRett }) Avgjørelse.Innvilgelse(perioder) else Avgjørelse.Avslag
        nye.isEmpty() -> Avgjørelse.Endring(perioder)
        arvede.last().harRett && !nye.any { it.harRett } -> Avgjørelse.Stans(perioder)
        !arvede.last().harRett && !nye.any { it.harRett } -> Avgjørelse.Avslag
        !arvede.last().harRett && nye.any { it.harRett } -> Avgjørelse.Gjenopptak(perioder)
        else -> Avgjørelse.Endring(perioder)
    }
}

private fun ferietilleggUtbetalinger(opplysninger: LesbarOpplysninger): List<Utbetaling> {
    if (opplysninger.mangler(FerietilleggBeløp.ferietilleggBeløp)) return emptyList()
    val ferietilleggBeløp = opplysninger.finnOpplysning(FerietilleggBeløp.ferietilleggBeløp)
    val opptjeningsår = opplysninger.finnOpplysning(KravPåFerietillegg.åretDetSkalBeregnesFerietilleggFor).verdi

    val utbetaling =
        Utbetaling(
            meldeperiode = "Ferietillegg-$opptjeningsår",
            dato = LocalDate.of(opptjeningsår + 1, 5, 1),
            sats = ferietilleggBeløp.verdi.verdien.toInt(),
            utbetaling = ferietilleggBeløp.verdi.verdien.toInt(),
            endret = true,
            ytelsestype = Ytelsestype("Ferietillegg"),
        )
    return listOf(utbetaling)
}
