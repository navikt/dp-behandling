package no.nav.dagpenger.ferietillegg

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.Utbetaling
import no.nav.dagpenger.opplysning.Utfall
import no.nav.dagpenger.opplysning.Ytelsestype
import java.time.LocalDate

val RegelverkFerietillegg =
    Regelverk(
        navn = RegelverkType("Ferietillegg"),
        rettighetsperiodeberegning = ::ferietilleggRettighetsperioder,
        utbetalingsberegning = ::ferietilleggUtbetalinger,
        utfallberegning = ::ferietilleggUtfall,
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

private fun ferietilleggUtfall(opplysninger: LesbarOpplysninger): Utfall {
    val perioder = ferietilleggRettighetsperioder(opplysninger)
    if (perioder.isEmpty()) return Utfall.Uavklart

    val (nye, arvede) = perioder.partition { it.endret }

    return when {
        arvede.isEmpty() -> if (nye.any { it.harRett }) Utfall.Innvilgelse(perioder) else Utfall.Avslag
        nye.isEmpty() -> Utfall.Endring(perioder)
        arvede.last().harRett && !nye.any { it.harRett } -> Utfall.Stans(perioder)
        !arvede.last().harRett && !nye.any { it.harRett } -> Utfall.Avslag
        !arvede.last().harRett && nye.any { it.harRett } -> Utfall.Gjenopptak(perioder)
        else -> Utfall.Endring(perioder)
    }
}

private fun ferietilleggUtbetalinger(opplysninger: LesbarOpplysninger): List<Utbetaling> {
    val kravPåFerietillegg = opplysninger.finnOpplysning(KravPåFerietillegg.harKravpåFerietillegg)
    if (!kravPåFerietillegg.verdi) return emptyList()

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
