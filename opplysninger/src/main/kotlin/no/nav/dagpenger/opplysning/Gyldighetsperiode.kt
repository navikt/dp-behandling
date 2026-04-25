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
        require(fraOgMed <= tilOgMed) { "fraOgMed=$fraOgMed må være før tilOgMed=$tilOgMed" }
        require(tilOgMed == LocalDate.MAX || tilOgMed.year < 200_000) {
            "Hvis tilOgMed ikke er MAX må den ikke være tusenvis av år inn i framtiden"
        }
    }

    fun inneholder(dato: LocalDate) = dato in this

    /**
     * Har perioden en eksplisitt startdato (fraOgMed er ikke MIN)?
     */
    val harStartdato get() = fraOgMed != LocalDate.MIN

    /**
     * Har perioden en eksplisitt sluttdato (tilOgMed er ikke MAX)?
     */
    val harSluttdato get() = tilOgMed != LocalDate.MAX

    /**
     * Er perioden ubegrenset (gyldig for alltid)?
     */
    val erUbegrenset get() = !harStartdato && !harSluttdato

    fun overlapper(gyldighetsperiode: Gyldighetsperiode) =
        this.contains(gyldighetsperiode.fraOgMed) || gyldighetsperiode.contains(this.fraOgMed)

    /**
     * Ligger denne perioden helt før [other], uten overlapp?
     */
    fun erFør(other: Gyldighetsperiode) = tilOgMed < other.fraOgMed

    /**
     * Ligger denne perioden helt etter [other], uten overlapp?
     */
    fun erEtter(other: Gyldighetsperiode) = fraOgMed > other.tilOgMed

    /**
     * Ligger denne perioden kant-i-kant med [other]? (dagen etter/før, uten overlapp)
     */
    fun tilstøter(other: Gyldighetsperiode) =
        (tilOgMed != LocalDate.MAX && tilOgMed.plusDays(1) == other.fraOgMed) ||
            (other.tilOgMed != LocalDate.MAX && other.tilOgMed.plusDays(1) == fraOgMed)

    /**
     * Beregn den overlappende perioden (intersection) av to perioder, eller null om de ikke overlapper.
     */
    fun overlapp(other: Gyldighetsperiode): Gyldighetsperiode? {
        val nyFom = maxOf(fraOgMed, other.fraOgMed)
        val nyTom = minOf(tilOgMed, other.tilOgMed)
        return if (nyFom <= nyTom) Gyldighetsperiode(nyFom, nyTom) else null
    }

    /**
     * Trekk fra en annen periode. Returnerer 0-2 segmenter som dekker
     * delene av denne perioden som IKKE overlapper med [other].
     */
    operator fun minus(other: Gyldighetsperiode): List<Gyldighetsperiode> {
        if (!overlapper(other)) return listOf(this)

        val resultat = mutableListOf<Gyldighetsperiode>()

        // Segment til venstre: denne starter før other
        if (fraOgMed < other.fraOgMed && other.fraOgMed != LocalDate.MIN) {
            resultat.add(Gyldighetsperiode(fraOgMed, other.fraOgMed.minusDays(1)))
        }

        // Segment til høyre: denne slutter etter other
        if (tilOgMed > other.tilOgMed && other.tilOgMed != LocalDate.MAX) {
            resultat.add(Gyldighetsperiode(other.tilOgMed.plusDays(1), tilOgMed))
        }

        return resultat
    }

    /**
     * Trekk fra flere perioder. Returnerer segmentene som IKKE dekkes av noen av periodene.
     */
    fun minus(andre: Collection<Gyldighetsperiode>): List<Gyldighetsperiode> {
        val sortert = andre.sortedBy { it.fraOgMed }
        var segmenter = listOf(this)

        for (periode in sortert) {
            segmenter = segmenter.flatMap { it - periode }
        }

        return segmenter
    }

    override fun toString(): String =
        when {
            erUbegrenset -> "gyldig for alltid"
            !harStartdato -> "gyldig til $tilOgMed"
            !harSluttdato -> "gyldig fra $fraOgMed"
            else -> "gyldig fra $fraOgMed til $tilOgMed"
        }

    companion object {
        fun kun(dato: LocalDate) = Gyldighetsperiode(dato, dato)

        fun Collection<Gyldighetsperiode>.overlappendePerioder(): Boolean {
            val sorted = this.sortedBy { it.fraOgMed }
            return sorted.zipWithNext().any { (første, andre) -> første.overlapper(andre) }
        }
    }
}
