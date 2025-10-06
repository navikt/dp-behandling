package no.nav.dagpenger.opplysning.verdier

import java.time.LocalDate

class BarnListe(
    barn: List<Barn>,
) : ComparableListe<Barn>(barn) {
    override fun toString(): String = if (isEmpty()) "Ingen barn" else this.joinToString()
}

abstract class ComparableListe<T : Comparable<T>>(
    private val liste: List<T>,
) : Comparable<ComparableListe<T>>,
    List<T> by liste {
    override fun compareTo(other: ComparableListe<T>): Int = 0
}

data class Barn(
    val fødselsdato: LocalDate,
    val fornavnOgMellomnavn: String? = null,
    val etternavn: String? = null,
    val statsborgerskap: String? = null,
    val kvalifiserer: Boolean,
) : Comparable<Barn> {
    override fun compareTo(other: Barn): Int = this.fødselsdato.compareTo(other.fødselsdato)

    fun girBarnetillegg(
        dato: LocalDate,
        aldersgrense: Int,
    ) = kvalifiserer && dato.isBefore(fødselsdato.plusYears(aldersgrense.toLong()))

    override fun toString() =
        """Barn(fødselsdato=$fødselsdato, fornavnOgMellomnavn=$fornavnOgMellomnavn, 
            |etternavn=$etternavn, statsborgerskap=$statsborgerskap, kvalifiserer=$kvalifiserer)
        """.trimMargin()
}
