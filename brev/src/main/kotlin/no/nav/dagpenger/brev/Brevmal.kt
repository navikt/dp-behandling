package no.nav.dagpenger.brev

import java.util.UUID

data class Brevmal(
    val id: UUID = UUID.randomUUID(),
    val navn: String,
    val maltekster: List<Maltekst>,
)
