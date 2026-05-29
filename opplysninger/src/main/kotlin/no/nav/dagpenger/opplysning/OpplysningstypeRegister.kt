package no.nav.dagpenger.opplysning

/**
 * Et immutable oppslag av kjente [Opplysningstype]-er per [Opplysningstype.Id].
 *
 * Brukes av infrastruktur (f.eks. repositories) for å koble persistert data tilbake
 * til typedefinisjoner i kode uten å være avhengig av global, mutable tilstand.
 *
 * Bygg én instans ved oppstart fra unionen av alle registrerte regelverk
 * og injiser den der den trengs.
 */
interface OpplysningstypeRegister {
    operator fun get(id: Opplysningstype.Id<*>): Opplysningstype<*>?

    val alle: Set<Opplysningstype<*>>

    companion object {
        val tom: OpplysningstypeRegister = av(emptySet())

        fun av(opplysningstyper: Collection<Opplysningstype<*>>): OpplysningstypeRegister =
            InMemoryOpplysningstypeRegister(opplysningstyper.toSet())
    }
}

private class InMemoryOpplysningstypeRegister(
    override val alle: Set<Opplysningstype<*>>,
) : OpplysningstypeRegister {
    private val byId: Map<Opplysningstype.Id<*>, Opplysningstype<*>> = alle.associateBy { it.id }

    override fun get(id: Opplysningstype.Id<*>): Opplysningstype<*>? = byId[id]
}
