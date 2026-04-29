package no.nav.dagpenger.brev

data class Brev(
    val overskrift: String,
    val seksjoner: List<Brevseksjon>,
)

data class Brevseksjon(
    val plassering: Plassering,
    val innhold: List<String>,
)
