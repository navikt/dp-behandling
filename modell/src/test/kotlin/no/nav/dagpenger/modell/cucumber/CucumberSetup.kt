package no.nav.dagpenger.modell.cucumber

import io.cucumber.java8.No
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.lang.reflect.Type

class CucumberSetup : No {
    companion object {
        private val objectMapper = jacksonObjectMapper()
    }

    init {
        DefaultParameterTransformer { fromValue: String?, toValueType: Type? ->
            objectMapper.convertValue(
                fromValue,
                objectMapper.constructType(toValueType),
            )
        }
        DefaultDataTableCellTransformer { fromValue: String?, toValueType: Type? ->
            objectMapper.convertValue(
                fromValue,
                objectMapper.constructType(toValueType),
            )
        }
        DefaultDataTableEntryTransformer { fromValue: Map<String?, String?>?, toValueType: Type? ->
            objectMapper.convertValue(
                fromValue,
                objectMapper.constructType(toValueType),
            )
        }
    }
}
