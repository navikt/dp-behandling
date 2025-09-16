package no.nav.dagpenger.opplysning

import java.time.LocalDate

data class Gyldighetsperiode(
    val fraOgMed: LocalDate = LocalDate.MIN,
    val tilOgMed: LocalDate = LocalDate.MAX,
    private val range: ClosedRange<LocalDate> = fraOgMed..tilOgMed,
) : ClosedRange<LocalDate> by range {
    constructor(fom: LocalDate) : this(fom, LocalDate.MAX)

    /*init {
        require(fraOgMed.isEqual(tilOgMed) || fraOgMed.isBefore(tilOgMed)) { "tilOgMed=$tilOgMed kan ikke være før fraOgMed=$fraOgMed" }
    }*/

    fun inneholder(dato: LocalDate) = dato in range

    fun overlapp(gyldighetsperiode: Gyldighetsperiode) =
        this.contains(gyldighetsperiode.fraOgMed) ||
            this.contains(gyldighetsperiode.fraOgMed) ||
            gyldighetsperiode.contains(this.fraOgMed) ||
            gyldighetsperiode.contains(this.fraOgMed)

    override fun toString(): String =
        when {
            fraOgMed.isEqual(LocalDate.MIN) && tilOgMed.isEqual(LocalDate.MAX) -> "gyldig for alltid"
            fraOgMed.isEqual(LocalDate.MIN) -> "gyldig til $tilOgMed"
            tilOgMed.isEqual(LocalDate.MAX) -> "gyldig fra $fraOgMed"
            else -> "gyldig fra $fraOgMed til $tilOgMed"
        }

    fun kopi(tom: LocalDate) = Gyldighetsperiode(fraOgMed, tom)

    companion object {
        fun kun(dato: LocalDate) = Gyldighetsperiode(dato, dato)

        fun Collection<Gyldighetsperiode>.overlappendePerioder(): Boolean {
            val sorted = this.sortedBy { it.fraOgMed }
            return sorted.zipWithNext().any { (første, andre) -> første.overlapp(andre) }
        }
    }
}
