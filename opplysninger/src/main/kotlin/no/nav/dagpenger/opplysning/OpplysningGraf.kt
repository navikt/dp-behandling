package no.nav.dagpenger.opplysning

import java.util.UUID

class OpplysningGraf(
    private val opplysninger: Collection<Opplysning<*>>,
) {
    // Bygg opp et omvendt indeks-kart: Hvilke opplysninger er utledet av hvilke
    private val utledetAvMap: Map<UUID, List<Opplysning<*>>> by lazy {
        opplysninger
            .flatMap { opplysning ->
                opplysning.utledetAv?.opplysninger?.map { it.id to opplysning } ?: emptyList()
            }.groupBy({ it.first }, { it.second })
    }

    private val utlededeOpplysninger by lazy { utledetAvMap.values.flatten().toSet() }

    /**
     * Returnerer en liste over alle opplysninger som er (direkte eller indirekte) utledet av [rot].
     */
    fun hentAlleUtledetAv(rot: Opplysning<*>): Set<Opplysning<*>> {
        val resultat = mutableSetOf<Opplysning<*>>()
        val stack = ArrayDeque<Opplysning<*>>()
        stack += rot

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            val barn = utledetAvMap[current.id].orEmpty()
            for (b in barn) {
                if (resultat.add(b)) {
                    stack += b
                }
            }
        }

        return resultat
    }
}
