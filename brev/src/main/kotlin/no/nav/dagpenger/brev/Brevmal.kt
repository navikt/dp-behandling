package no.nav.dagpenger.brev

import java.util.UUID

data class Brevmal(
    val id: UUID = UUID.randomUUID(),
    val navn: String,
    val maltekster: List<Maltekst>,
    /**
     * Plasseringer som må ha innhold for at brevet skal produseres.
     * Hvis ingen av disse har aktivert innhold, returnerer BrevBygger null.
     * Tom liste = alltid produser brev.
     */
    val krevInnholdI: Set<Plassering> = emptySet(),
)
