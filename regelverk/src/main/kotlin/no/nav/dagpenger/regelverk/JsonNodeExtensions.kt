package no.nav.dagpenger.regelverk

import tools.jackson.databind.JsonNode
import java.util.UUID

fun JsonNode.asUUID(): UUID = this.asString().let { UUID.fromString(it) }
