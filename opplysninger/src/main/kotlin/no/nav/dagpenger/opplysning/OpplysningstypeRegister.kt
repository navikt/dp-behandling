package no.nav.dagpenger.opplysning

/**
 * Et immutable oppslag av kjente [Opplysningstype]-er per [Opplysningstype.Id].
 *
 * Brukes av infrastruktur (f.eks. repositories) for å koble persistert data tilbake
 * til typedefinisjoner i kode uten å være avhengig av global, mutable tilstand.
 *
 * Konstruktøren validerer at ingen UUID brukes av flere ulike opplysningstyper.
 */
class OpplysningstypeRegister(
    opplysningstyper: Collection<Opplysningstype<*>>,
    historiskeOpplysningsIder: Collection<Opplysningstype.Id<*>> = emptyList(),
) {
    private val byId: Map<Opplysningstype.Id<*>, Opplysningstype<*>> = opplysningstyper.associateBy { it.id }

    init {
        val alle = opplysningstyper.map { it.id } + historiskeOpplysningsIder
        val duplikater =
            alle
                .groupBy { it.uuid }
                .filterValues { it.size > 1 }
        check(duplikater.isEmpty()) {
            val visning =
                duplikater.entries.joinToString("; ") { (uuid, id) ->
                    val type = byId.getValue(id.first())
                    "UUID $uuid brukes av: ${type.navn}(${type.datatype})"
                }
            "Flere opplysningstyper deler samme UUID: $visning"
        }
    }

    operator fun get(id: Opplysningstype.Id<*>): Opplysningstype<*>? = byId[id]

    companion object {
        val tom: OpplysningstypeRegister = OpplysningstypeRegister(emptySet(), emptyList())
    }
}
