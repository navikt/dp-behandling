package no.nav.dagpenger.opplysning

sealed interface Aktør {
    val ident: String
}

data class Saksbehandler(
    override val ident: String,
) : Aktør {
    init {
        require(ident.isNotBlank()) { "Saksbehandlerident kan ikke være tom" }
    }
}

data class Systemaktør(
    override val ident: String,
) : Aktør {
    init {
        require(ident.isNotBlank()) { "Systemident kan ikke være tom" }
    }

    companion object {
        val dpSak = Systemaktør("dp-sak")
    }
}
