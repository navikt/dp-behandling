package no.nav.dagpenger.utestengning

import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.RegelverkType
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.opplysning.Utbetalingsberegning
import no.nav.dagpenger.utestengning.UtestengningVilkår.erUtestengt
import no.nav.dagpenger.utestengning.UtestengningVilkår.fraOgMed
import no.nav.dagpenger.utestengning.UtestengningVilkår.tilOgMed

val RegelverkUtestengning =
    Regelverk(
        navn = RegelverkType("Utestengning"),
        rettighetsperiodeberegning = ::utestengningRettighetsperioder,
        utbetalingsberegning = Utbetalingsberegning { emptyList() },
        avgjørelsesberegning = ::utestengningAvgjørelse,
        UtestengningVilkår.regelsett,
    )

private fun utestengningRettighetsperioder(opplysninger: LesbarOpplysninger): List<Rettighetsperiode> {
    if (!opplysninger.har(erUtestengt) || !opplysninger.har(fraOgMed) || !opplysninger.har(tilOgMed)) {
        return emptyList()
    }

    val utestengtOpplysning = opplysninger.finnOpplysning(erUtestengt)
    return listOf(
        Rettighetsperiode(
            fraOgMed = opplysninger.finnOpplysning(fraOgMed).verdi,
            tilOgMed = opplysninger.finnOpplysning(tilOgMed).verdi,
            harRett = utestengtOpplysning.verdi,
            endret = true,
        ),
    )
}

private fun utestengningAvgjørelse(opplysninger: LesbarOpplysninger): Avgjørelse {
    val perioder = utestengningRettighetsperioder(opplysninger)
    if (perioder.isEmpty()) return Avgjørelse.Uavklart

    return if (perioder.any { it.harRett }) {
        Avgjørelse.Innvilgelse(perioder)
    } else {
        Avgjørelse.Avslag
    }
}
