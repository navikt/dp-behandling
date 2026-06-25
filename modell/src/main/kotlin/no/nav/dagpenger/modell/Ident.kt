package no.nav.dagpenger.modell

class Ident(
    private val ident: String,
    private val aliaser: List<String> = emptyList(),
) {
    init {
        require(ident.matches(Regex("[0-9]{11}"))) { "Personident må ha 11 siffer" }
        require(aliaser.all { it.matches(Regex("[0-9]{11}")) }) { "Alle aliaser må ha 11 siffer" }
    }

    companion object {
        fun String.tilPersonIdentfikator() = Ident(this)
    }

    fun identifikator() = ident

    fun alleIdentifikatorer(): List<String> = listOf(ident) + aliaser

    override fun equals(other: Any?) = other is Ident && ident == other.ident

    override fun hashCode() = ident.hashCode()

    override fun toString(): String = "Ident(${ident.substring(0, 6)}*****)"
}
