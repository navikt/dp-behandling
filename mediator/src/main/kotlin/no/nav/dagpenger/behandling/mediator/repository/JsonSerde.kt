package no.nav.dagpenger.behandling.mediator.repository

import no.nav.dagpenger.behandling.mediator.objectMapper
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.ObjectReader
import tools.jackson.databind.ObjectWriter
import java.io.InputStream

class JsonSerde<T>(
    private val reader: ObjectReader,
    private val writer: ObjectWriter,
) {
    fun toJson(value: T): String = writer.writeValueAsString(value)

    fun fromJson(json: String): T = reader.readValue(json)

    fun fromJson(json: JsonNode): T = reader.readValue(json)

    fun fromJson(json: InputStream): T = reader.readValue(json)

    companion object {
        inline fun <reified T> ObjectMapper.serde(): JsonSerde<T> {
            val type = object : TypeReference<T>() {}.type
            val javaType = typeFactory.constructType(type)
            return JsonSerde(
                reader = objectMapper.readerFor(javaType),
                writer = objectMapper.writerFor(javaType),
            )
        }
    }
}
