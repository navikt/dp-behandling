package no.nav.dagpenger.behandling

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.blackbird.BlackbirdModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

val objectMapper: ObjectMapper =
    jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(BlackbirdModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        // OpenAPI-generator klarer ikke optional-felter. Derfor må vi eksplisitt fjerne null-verdier
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
