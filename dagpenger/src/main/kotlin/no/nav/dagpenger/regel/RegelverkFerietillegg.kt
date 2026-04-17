package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.RettighetsperiodeStrategi
import no.nav.dagpenger.opplysning.Utbetaling
import no.nav.dagpenger.opplysning.UtbetalingerStrategi
import java.time.LocalDate

val RegelverkFerietillegg =
    Regelverk(
        KravPåFerietillegg.regelsett,
        FerietilleggBeløp.regelsett,
        rettighetsperiodeStrategi = FerietilleggRettighetsperiodeStrategi(),
        utbetalingerStrategi = FerietilleggUtbetalingStrategi(),
    )

class FerietilleggRettighetsperiodeStrategi : RettighetsperiodeStrategi {
    override fun rettighetsperioder(opplysninger: LesbarOpplysninger): List<Rettighetsperiode> {
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
}

class FerietilleggUtbetalingStrategi : UtbetalingerStrategi {
    override fun utbetalinger(opplysninger: LesbarOpplysninger): List<Utbetaling> {
        val kravPåFerietillegg = opplysninger.finnOpplysning(KravPåFerietillegg.harKravpåFerietillegg)
        if (!kravPåFerietillegg.verdi) return emptyList()

        val ferietilleggBeløp = opplysninger.finnOpplysning(FerietilleggBeløp.ferietilleggBeløp)
        val opptjeningsår = opplysninger.finnOpplysning(KravPåFerietillegg.åretDetSkalBeregnesFerietilleggFor).verdi

        val utbetaling =
            Utbetaling.Ferietillegg(
                dato = LocalDate.of(opptjeningsår + 1, 5, 1),
                utbetaling = ferietilleggBeløp.verdi.verdien.toInt(),
                endret = true,
            )
        return listOf(utbetaling)
    }
}
