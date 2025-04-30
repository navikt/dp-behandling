package no.nav.dagpenger.behandling.modell

data class Ident(
    private val ident: String,
) {
    init {
        require(ident.matches(Regex("[0-9]{11}"))) { "Personident må ha 11 siffer" }
    }

    companion object {
        fun String.tilPersonIdentfikator() = Ident(this)
    }

    fun identifikator() = ident

    fun alleIdentifikatorer() = listOf(ident)

    override fun toString(): String = "Ident(${ident.substring(0, 6)}*****)"
}
