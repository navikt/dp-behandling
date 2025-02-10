package no.nav.dagpenger.opplysning

import java.net.URI

data class Hjemmel(
    val kilde: Lovkilde,
    val kapittel: Int,
    val paragraf: Int,
    val tittel: String,
    val kortnavn: String,
) {
    val url = kilde.url?.resolve("§$kapittel-$paragraf")

    override fun toString() = "§ $kapittel-$paragraf. $tittel"
}

data class Lovkilde(
    val navn: String,
    val kortnavn: String,
    val url: URI? = null,
) {
    fun hjemmel(
        kapittel: Int,
        paragraf: Int,
        tittel: String,
        kortnavn: String,
    ) = Hjemmel(this, kapittel, paragraf, tittel, kortnavn)

    override fun toString() = kortnavn
}
