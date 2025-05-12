package no.nav.dagpenger.behandling.modell.hendelser

import java.util.UUID

sealed class EksternId<T>(
    val id: T,
) {
    val type: String = this::class.simpleName!!

    val datatype =
        when (id) {
            is UUID -> "UUID"
            is Long -> "Long"
            else -> throw IllegalArgumentException("Ukjent idType: $id")
        }

    abstract fun kontekstMap(): Map<String, String>

    override fun equals(other: Any?): Boolean = other is EksternId<*> && id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString() = "${this.javaClass.simpleName}($id)"

    companion object {
        fun fromString(
            eksternIdType: String,
            id: String,
        ) = when (eksternIdType) {
            "SøknadId" -> SøknadId(UUID.fromString(id))
            "MeldekortId" -> MeldekortId(id.toLong())
            else -> throw IllegalArgumentException("Ukjent idType: $eksternIdType")
        }
    }
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

class KnappenId(
    id: UUID,
) : EksternId<UUID>(id) {
    override fun kontekstMap() =
        mapOf(
            "knappenId" to id.toString(),
        )
}
