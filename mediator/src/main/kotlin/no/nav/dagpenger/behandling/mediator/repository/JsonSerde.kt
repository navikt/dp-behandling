package no.nav.dagpenger.behandling.mediator.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import java.io.InputStream

class JsonSerde<T>(
    private val reader: ObjectReader,
    private val writer: ObjectWriter,
) {
    fun toJson(value: T) = writer.writeValueAsString(value)

    fun fromJson(json: String): T = reader.readValue(json)

    fun fromJson(json: InputStream): T = reader.readValue(json)

    companion object {
        inline fun <reified T> ObjectMapper.serde(): JsonSerde<T> {
            val type = object : TypeReference<T>() {}.type
            val javaType = typeFactory.constructType(type)
            return JsonSerde(
                reader = readerFor(javaType),
                writer = writerFor(javaType),
            )
        }
    }
}
