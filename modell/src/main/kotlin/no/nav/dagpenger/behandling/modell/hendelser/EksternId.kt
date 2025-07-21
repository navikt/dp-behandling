package no.nav.dagpenger.behandling.modell.hendelser

import java.util.UUID

sealed class EksternId<T>(
    val id: T,
) {
    val type: String = this::class.simpleName!!

    val datatype =
        when (id) {
            is UUID -> "UUID"
            is String -> "String"
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
            "SøknadId" -> SøknadId(id)
            "MeldekortId" -> MeldekortId(id)
            "ManuellId" -> ManuellId(id)
            else -> throw IllegalArgumentException("Ukjent idType: $eksternIdType")
        }
    }
}

class SøknadId(
    id: UUID,
) : EksternId<UUID>(id) {
    constructor(id: String) : this(UUID.fromString(id))

    override fun kontekstMap() =
        mapOf(
            "søknadId" to id.toString(),
            "søknad_uuid" to id.toString(),
        )
}

class MeldekortId(
    id: String,
) : EksternId<String>(id) {
    override fun kontekstMap() =
        mapOf(
            "meldekortId" to id,
        )
}

class ManuellId(
    id: UUID,
) : EksternId<UUID>(id) {
    constructor(id: String) : this(UUID.fromString(id))

    override fun kontekstMap() =
        mapOf(
            "manuellId" to id.toString(),
        )
}
