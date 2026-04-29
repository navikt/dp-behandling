package no.nav.dagpenger.brev

import java.util.UUID

data class Maltekst(
    val id: UUID = UUID.randomUUID(),
    val trigger: Trigger,
    val tekst: String,
    val plassering: Plassering,
    val rekkefølge: Int,
)
