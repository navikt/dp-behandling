package no.nav.dagpenger.opplysning.verdier

data class Ulid(
    val verdi: String,
) : Comparable<Ulid> {
    init {
        require(verdi.length == 26) { "ULID krever en 26 tegn lang streng." }
    }

    override fun compareTo(other: Ulid) = verdi.compareTo(other.verdi)

    override fun toString() = verdi
}
