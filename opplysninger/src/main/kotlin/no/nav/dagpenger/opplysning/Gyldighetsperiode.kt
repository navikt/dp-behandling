package no.nav.dagpenger.opplysning

import java.time.LocalDate

data class Gyldighetsperiode(
    val fraOgMed: LocalDate = LocalDate.MIN,
    val tilOgMed: LocalDate = LocalDate.MAX,
) : ClosedRange<LocalDate> {
    constructor(fom: LocalDate) : this(fom, LocalDate.MAX)

    override val start = fraOgMed
    override val endInclusive = tilOgMed

    init {
        require(fraOgMed.isEqual(tilOgMed) || fraOgMed.isBefore(tilOgMed)) { "fraOgMed=$fraOgMed må være før tilOgMed=$tilOgMed" }
        require(tilOgMed.isEqual(LocalDate.MAX) || tilOgMed.year < 200000) {
            "Hvis tilOgMed ikke er MAX må den ikke være tusenvis av år inn i framtiden"
        }
    }

    fun inneholder(dato: LocalDate) = dato in this

    fun overlapp(gyldighetsperiode: Gyldighetsperiode) =
        this.contains(gyldighetsperiode.fraOgMed) || gyldighetsperiode.contains(this.fraOgMed)

    override fun toString(): String =
        when {
            fraOgMed.isEqual(LocalDate.MIN) && tilOgMed.isEqual(LocalDate.MAX) -> "gyldig for alltid"
            fraOgMed.isEqual(LocalDate.MIN) -> "gyldig til $tilOgMed"
            tilOgMed.isEqual(LocalDate.MAX) -> "gyldig fra $fraOgMed"
            else -> "gyldig fra $fraOgMed til $tilOgMed"
        }

    companion object {
        fun kun(dato: LocalDate) = Gyldighetsperiode(dato, dato)

        fun Collection<Gyldighetsperiode>.overlappendePerioder(): Boolean {
            val sorted = this.sortedBy { it.fraOgMed }
            return sorted.zipWithNext().any { (første, andre) -> første.overlapp(andre) }
        }
    }
}
