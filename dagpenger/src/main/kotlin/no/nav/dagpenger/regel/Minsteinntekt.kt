package no.nav.dagpenger.regel

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.id
import no.nav.dagpenger.opplysning.regel.enAv
import no.nav.dagpenger.opplysning.regel.innhentMed
import no.nav.dagpenger.opplysning.regel.multiplikasjon
import no.nav.dagpenger.opplysning.regel.oppslag
import no.nav.dagpenger.opplysning.regel.størreEnnEllerLik
import java.time.LocalDate

object Minsteinntekt {
    val nedreTerskelFaktor = Opplysningstype.somDesimaltall("Antall G for krav til 12 mnd inntekt")
    val øvreTerskelFaktor = Opplysningstype.somDesimaltall("Antall G for krav til 36 mnd inntekt")
    val inntekt12 = Opplysningstype.somDesimaltall("Inntekt siste 12 mnd".id("InntektSiste12Mnd"))
    val inntekt36 = Opplysningstype.somDesimaltall("Inntekt siste 36 mnd".id("InntektSiste3År"))
    val grunnbeløp = Opplysningstype.somDesimaltall("Grunnbeløp")

    private val virkningsdato = Virkningsdato.virkningsdato
    private val nedreTerskel = Opplysningstype.somDesimaltall("Inntektskrav for siste 12 mnd")
    private val øvreTerskel = Opplysningstype.somDesimaltall("Inntektskrav for siste 36 mnd")
    private val overNedreTerskel = Opplysningstype.somBoolsk("Inntekt er over kravet for siste 12 mnd")
    private val overØvreTerskel = Opplysningstype.somBoolsk("Inntekt er over kravet for siste 36 mnd")

    val minsteinntekt = Opplysningstype.somBoolsk("Minsteinntekt")

    val regelsett =
        Regelsett("Minsteinntekt") {
            regel(inntekt12) { innhentMed(virkningsdato) }
            regel(inntekt36) { innhentMed(virkningsdato) }
            regel(grunnbeløp) { oppslag(virkningsdato) { Grunnbeløp.finnFor(it) } }
            regel(nedreTerskelFaktor) { oppslag(virkningsdato) { 1.5 } }
            regel(øvreTerskelFaktor) { oppslag(virkningsdato) { 3.0 } }
            regel(nedreTerskel) { multiplikasjon(nedreTerskelFaktor, grunnbeløp) }
            regel(øvreTerskel) { multiplikasjon(øvreTerskelFaktor, grunnbeløp) }
            regel(overNedreTerskel) { størreEnnEllerLik(inntekt12, nedreTerskel) }
            regel(overØvreTerskel) { størreEnnEllerLik(inntekt36, øvreTerskel) }
            regel(minsteinntekt) { enAv(overNedreTerskel, overØvreTerskel) }
        }
}

internal object Grunnbeløp {
    const val TEST_GRUNNBELØP = 118620.0
    private val grunnbeløp =
        mapOf(
            LocalDate.of(2020, 5, 1) to 99858.0,
            LocalDate.of(2021, 5, 1) to 105144.0,
            LocalDate.of(2022, 5, 1) to TEST_GRUNNBELØP,
        )

    fun finnFor(dato: LocalDate) =
        grunnbeløp.filterKeys { it <= dato }.maxByOrNull { it.key }?.value
            ?: throw IllegalArgumentException("Fant ikke grunnbeløp for $dato")
}

/*
// Forstår terskelverdier for minste arbeidsinntekt
object MinstearbeidsinntektFaktorStrategi {
    private val terskler =
        listOf(
            MinstearbeidsinntektTerskel(
                20.mars(2020),
                30.oktober(2020),
                Faktor(0.75, 2.25),
            ), // Forskrift § 2-2.Midlertidig krav til minsteinntekt – unntak fra folketrygdloven § 4-4
            MinstearbeidsinntektTerskel(
                19.februar(2021),
                30.september(2021),
                Faktor(0.75, 2.25),
            ),
            MinstearbeidsinntektTerskel(
                19.februar(2021),
                30.september(2021),
                Faktor(0.75, 2.25),
            ), // Forskrift § 2-2.Midlertidig krav til minsteinntekt – unntak fra folketrygdloven § 4-4
            MinstearbeidsinntektTerskel(
                15.desember(2021),
                31.mars(2022),
                Faktor(0.75, 2.25),
            ),
            MinstearbeidsinntektTerskel(
                LocalDate.MIN,
                LocalDate.MAX,
                Faktor(1.5, 3.0),
            ), // https://lovdata.no/lov/1997-02-28-19/§4-4
        )

    fun finnFaktor(virkningsdato: LocalDate): Faktor {
        return terskler.first { virkningsdato in it }.faktor
    }

    private class MinstearbeidsinntektTerskel constructor(
        private val fom: LocalDate,
        private val tom: LocalDate,
        val faktor: Faktor,
    ) : ClosedRange<LocalDate> {
        init {
            require(fom.isBefore(tom)) { "Til og med '$fom' må være før Fra-og-med '$tom'" }
        }

        override val endInclusive: LocalDate
            get() = tom
        override val start: LocalDate
            get() = fom
    }

    data class Faktor(
        val nedre: Double,
        val øvre: Double,
    )
}

private fun Int.januar(year: Int) = LocalDate.of(year, 1, this)

private fun Int.februar(year: Int) = LocalDate.of(year, 2, this)

private fun Int.mars(year: Int) = LocalDate.of(year, 3, this)

private fun Int.april(year: Int) = LocalDate.of(year, 4, this)

private fun Int.mai(year: Int) = LocalDate.of(year, 5, this)

private fun Int.juni(year: Int) = LocalDate.of(year, 6, this)

private fun Int.juli(year: Int) = LocalDate.of(year, 7, this)

private fun Int.august(year: Int) = LocalDate.of(year, 8, this)

private fun Int.september(year: Int) = LocalDate.of(year, 9, this)

private fun Int.oktober(year: Int) = LocalDate.of(year, 10, this)

private fun Int.november(year: Int) = LocalDate.of(year, 11, this)

private fun Int.desember(year: Int) = LocalDate.of(year, 12, this)
*/
