package no.nav.dagpenger.opplysning

import java.time.LocalDate

data class Gyldighetsperiode(
    val fom: LocalDate = LocalDate.MIN,
    val tom: LocalDate = LocalDate.MAX,
    private val range: ClosedRange<LocalDate> = fom..tom,
) : ClosedRange<LocalDate> by range {
    constructor(fom: LocalDate) : this(fom, LocalDate.MAX)

    val erUendelig = fom == LocalDate.MIN && tom == LocalDate.MAX

    fun inneholder(dato: LocalDate) = dato in range

    fun overlapp(gyldighetsperiode: Gyldighetsperiode) =
        this.contains(gyldighetsperiode.fom) ||
            this.contains(gyldighetsperiode.fom) ||
            gyldighetsperiode.contains(this.fom) ||
            gyldighetsperiode.contains(this.fom)

    override fun toString(): String =
        when {
            fom == LocalDate.MIN && tom == LocalDate.MAX -> "gyldig for alltid"
            fom == LocalDate.MIN -> "gyldig til $tom"
            tom == LocalDate.MAX -> "gyldig fra $fom"
            else -> "gyldig fra $fom til $tom"
        }

    fun kopi(tom: LocalDate) = Gyldighetsperiode(fom, tom)
}
