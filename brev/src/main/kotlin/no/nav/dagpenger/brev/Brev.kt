package no.nav.dagpenger.brev

data class Brev(
    val overskrift: String,
    val seksjoner: List<Brevseksjon>,
)

data class Brevseksjon(
    val plassering: Plassering,
    val tittel: String? = null,
    val innhold: List<String>,
)
