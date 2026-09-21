package no.nav.dagpenger.modell

data class Oppretter(
    val type: Type,
    val ident: String,
) {
    init {
        require(ident.isNotBlank()) { "Oppretterident kan ikke være tom" }
    }

    enum class Type {
        Saksbehandler,
        System,
        ;

        companion object {
            fun fraVerdi(verdi: String): Type =
                entries.singleOrNull { it.name == verdi }
                    ?: throw IllegalArgumentException("Ukjent opprettertype: $verdi")
        }
    }

    companion object {
        val dpSak = Oppretter(Type.System, "dp-sak")

        fun fra(
            type: String,
            ident: String,
        ): Oppretter = Oppretter(Type.fraVerdi(type), ident)
    }
}
