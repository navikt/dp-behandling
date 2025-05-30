package no.nav.dagpenger.opplysning

data class Avklaringkode(
    val kode: String,
    val tittel: String,
    val beskrivelse: String,
    val kanKvitteres: Boolean = true,
    val kanAvbrytes: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Avklaringkode) return false
        return kode == other.kode
    }

    override fun hashCode() = kode.hashCode()
}
