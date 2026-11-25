package no.nav.dagpenger.mediator

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import tools.jackson.databind.node.ObjectNode

fun toJsonMessage(
    eventName: String,
    dto: Any,
): JsonMessage {
    val node = objectMapper.valueToTree<ObjectNode>(dto)
    node.put("@event_name", eventName)
    val json = objectMapper.writeValueAsString(node)
    return JsonMessage(json, MessageProblems(json))
}
