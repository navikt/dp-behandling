package no.nav.dagpenger.behandling.mediator.melding

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import no.nav.dagpenger.behandling.mediator.IMessageMediator
import java.util.UUID

internal interface Melding {
    val id: UUID

    fun lagreMelding(repository: MeldingRepository)
}

internal abstract class KafkaMelding(
    private val packet: JsonMessage,
) : Melding {
    init {
        packet.interestedIn("@id", "@event_name", "@opprettet")
    }

    override val id: UUID = packet["@id"].asUUID()
    private val navn = packet["@event_name"].asText()
    internal val opprettet = packet["@opprettet"].asLocalDateTime()
    internal abstract val ident: String

    internal abstract fun behandle(
        mediator: IMessageMediator,
        context: MessageContext,
    )

    override fun lagreMelding(repository: MeldingRepository) {
        repository.lagreMelding(this, ident, id, toJson())
    }

    internal fun tracinginfo() =
        additionalTracinginfo(packet) +
            mapOf(
                "event_name" to navn,
                "id" to id.toString(),
                "opprettet" to opprettet.toString(),
            )

    protected open fun additionalTracinginfo(packet: JsonMessage): Map<String, String> = emptyMap()

    internal fun JsonNode.asUUID() = this.asText().let { UUID.fromString(it) }

    internal fun toJson() = packet.toJson()
}
