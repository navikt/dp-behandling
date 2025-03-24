package no.nav.dagpenger.behandling.scenario

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.dagpenger.behandling.objectMapper

internal class Behovsløsere(
    private val rapid: TestRapid,
    private val person: Mennesket,
) {
    private var lastOffset = 1

    private val sisteMeldingErBehov get() = rapid.inspektør.field(rapid.inspektør.size - 1, "@event_name").asText() == "behov"

    fun løsTilForslag() {
        while (sisteMeldingErBehov) {
            løsAktiveBehov()
        }
    }

    private fun løsAktiveBehov() {
        val aktiveBehov = aktiveBehov()
        val løsninger = person.løsningFor(aktiveBehov)
        lastOffset = rapid.inspektør.size
        rapid.sendTestMessage(løstBehov(løsninger), person.ident)
    }

    private fun løstBehov(løsninger: Map<String, Any>): String =
        rapid.inspektør.message(rapid.inspektør.size - 1).run {
            val løsningsobjekt = this as ObjectNode
            løsningsobjekt.put("@final", true)
            løsningsobjekt.putPOJO("@løsning", løsninger)
            objectMapper.writeValueAsString(løsningsobjekt)
        }

    private fun aktiveBehov(): List<String> {
        val nyeOffsets = lastOffset..<rapid.inspektør.size
        val nyeMeldinger = nyeOffsets.map { offset -> rapid.inspektør.message(offset) }
        val nyeBehov = nyeMeldinger.filter { it["@event_name"].asText() == "behov" }

        return nyeBehov.flatMap { melding -> melding["@behov"].map { behov -> behov.asText() } }
    }

    fun sisteForslag(): JsonNode {
        val sisteMelding = rapid.inspektør.message(rapid.inspektør.size - 1)
        require(sisteMelding["@event_name"].asText() == "forslag_til_vedtak")
        return sisteMelding
    }

    fun sisteVedtak(): JsonNode {
        val sisteMelding = rapid.inspektør.message(rapid.inspektør.size - 1)
        require(sisteMelding["@event_name"].asText() == "vedtak_fattet")
        return sisteMelding
    }
}
