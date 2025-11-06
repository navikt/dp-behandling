package no.nav.dagpenger.opplysning.verdier

import java.time.LocalDate
import java.util.UUID

data class BarnListe(
    val søknadbarnId: UUID? = null,
    val barn: List<Barn>,
) : Comparable<BarnListe> {
    override fun toString(): String = if (barn.isEmpty()) "Ingen barn" else this.barn.joinToString()

    override fun compareTo(other: BarnListe) = 0
}

data class Barn(
    val fødselsdato: LocalDate,
    val fornavnOgMellomnavn: String? = null,
    val etternavn: String? = null,
    val statsborgerskap: String? = null,
    val kvalifiserer: Boolean,
) : Comparable<Barn> {
    override fun compareTo(other: Barn): Int = this.fødselsdato.compareTo(other.fødselsdato)

    override fun toString() =
        """Barn(fødselsdato=$fødselsdato, fornavnOgMellomnavn=$fornavnOgMellomnavn, 
            |etternavn=$etternavn, statsborgerskap=$statsborgerskap, kvalifiserer=$kvalifiserer)
        """.trimMargin()
}
