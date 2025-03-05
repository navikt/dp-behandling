package no.nav.dagpenger.behandling.modell.hendelser

import java.util.UUID

sealed class EksternId<T>(
    val id: T,
) {
    val type: T = id

    abstract fun kontekstMap(): Map<String, String>

    override fun equals(other: Any?): Boolean = other is EksternId<*> && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString() = "${this.javaClass.simpleName}($id)"
}

class SøknadId(
    id: UUID,
) : EksternId<UUID>(id) {
    override fun kontekstMap() =
        mapOf(
            "søknadId" to id.toString(),
            "søknad_uuid" to id.toString(),
        )
}

class MeldekortId(
    id: Long,
) : EksternId<Long>(id) {
    override fun kontekstMap() =
        mapOf(
            "meldekortId" to id.toString(),
        )
}
