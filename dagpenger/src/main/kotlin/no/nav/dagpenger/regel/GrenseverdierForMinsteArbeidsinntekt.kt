package no.nav.dagpenger.regel

import no.nav.dagpenger.dato.desember
import no.nav.dagpenger.dato.februar
import no.nav.dagpenger.dato.mars
import no.nav.dagpenger.dato.oktober
import no.nav.dagpenger.dato.september
import java.time.LocalDate

// Forstår terskelverdier for minste arbeidsinntekt
object GrenseverdierForMinsteArbeidsinntekt {
    private val terskler =
        listOf(
            MinstearbeidsinntektTerskel(
                20.mars(2020),
                30.oktober(2020),
                Faktor(0.75, 2.25),
            ),
            // Forskrift § 2-2.Midlertidig krav til minsteinntekt – unntak fra folketrygdloven § 4-4
            MinstearbeidsinntektTerskel(
                19.februar(2021),
                30.september(2021),
                Faktor(0.75, 2.25),
            ),
            MinstearbeidsinntektTerskel(
                19.februar(2021),
                30.september(2021),
                Faktor(0.75, 2.25),
            ),
            // Forskrift § 2-2.Midlertidig krav til minsteinntekt – unntak fra folketrygdloven § 4-4
            MinstearbeidsinntektTerskel(
                15.desember(2021),
                31.mars(2022),
                Faktor(0.75, 2.25),
            ),
            MinstearbeidsinntektTerskel(
                LocalDate.MIN,
                LocalDate.MAX,
                Faktor(1.5, 3.0),
            ),
            // https://lovdata.no/lov/1997-02-28-19/§4-4
        )

    fun finnTerskel(virkningsdato: LocalDate): Faktor = terskler.first { virkningsdato in it }.faktor

    private class MinstearbeidsinntektTerskel(
        fom: LocalDate,
        tom: LocalDate,
        val faktor: Faktor,
        private val range: ClosedRange<LocalDate> = fom..tom,
    ) : ClosedRange<LocalDate> by range

    data class Faktor(
        val nedre: Double,
        val øvre: Double,
    )
}
