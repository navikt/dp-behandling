package no.nav.dagpenger.opplysning

data class Avklaringkode(
    val kode: String,
    val tittel: String,
    val beskrivelse: String,
    // Om saksbehandler kan selv kvittere avklaringen, eller om fakta MÅ endres
    val kanKvitteres: Boolean = true,
    // Om systemet kan automatisk avbryte avklaringen
    val kanAvbrytes: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Avklaringkode) return false
        return kode == other.kode
    }

    override fun hashCode() = kode.hashCode()
}
