package no.nav.dagpenger.regelverk.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import tools.jackson.databind.JsonNode
import java.util.UUID

abstract class KafkaMelding(
    private val packet: JsonMessage,
) : Melding {
    init {
        packet.interestedIn("@id", "@event_name", "@opprettet")
    }

    override val id: UUID = packet["@id"].asUUID()
    private val navn = packet["@event_name"].asString()
    val opprettet = packet["@opprettet"].asLocalDateTime()
    abstract val ident: String

    override fun lagreMelding(repository: MeldingRepository) {
        repository.lagreMelding(this, ident, id, toJson())
    }

    fun tracinginfo() =
        additionalTracinginfo(packet) +
            mapOf(
                "event_name" to navn,
                "id" to id.toString(),
                "opprettet" to opprettet.toString(),
            )

    protected open fun additionalTracinginfo(packet: JsonMessage): Map<String, String> = emptyMap()

    fun JsonNode.asUUID() = this.asString().let { UUID.fromString(it) }

    fun toJson() = packet.toJson()
}
