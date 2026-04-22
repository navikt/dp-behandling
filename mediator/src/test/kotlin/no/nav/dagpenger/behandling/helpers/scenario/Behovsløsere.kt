package no.nav.dagpenger.behandling.helpers.scenario

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
        var iterasjoner = 0
        val maksIterasjoner = 50
        while (sisteMeldingErBehov) {
            iterasjoner++
            check(iterasjoner < maksIterasjoner) {
                "Mulig uendelig behovsløkke etter $iterasjoner iterasjoner. Siste behov: ${aktiveBehov()}. Siste melding: \n ${
                    rapid.inspektør.message(
                        rapid.inspektør.size - 1,
                    )
                }"
            }
            løsAktiveBehov()
        }
    }

    private fun løsAktiveBehov() {
        val alleBehov = mutableMapOf<String, JsonNode>()
        val behovMeldinger = uløsteBehov()
        for (melding in behovMeldinger) {
            for (behovNavn in melding["@behov"].map { it.asText() }) {
                alleBehov[behovNavn] = melding
            }
        }
        val løsninger = person.løsningFor(alleBehov)
        lastOffset = rapid.inspektør.size
        rapid.sendTestMessage(løstBehov(løsninger), person.ident)
    }

    fun aktiveBehov(): List<String> =
        uløsteBehov().flatMap { melding ->
            melding["@behov"].map { it.asText() }
        }

    private fun uløsteBehov(): List<JsonNode> {
        val nyeOffsets = lastOffset..<rapid.inspektør.size
        return nyeOffsets
            .map { offset -> rapid.inspektør.message(offset) }
            .filter { it["@event_name"].asText() == "behov" }
    }

    private fun løstBehov(løsninger: Map<String, Any>): String =
        rapid.inspektør.message(rapid.inspektør.size - 1).run {
            val løsning = this as ObjectNode
            løsning.put("@final", true)
            løsning.putPOJO("@løsning", objectMapper.valueToTree(løsninger))
            objectMapper.writeValueAsString(løsning)
        }

    fun sisteBehandlingsresultatForslag() = rapid.inspektør.sisteMelding("forslag_til_behandlingsresultat")

    fun sisteBehandlingsresultat() = rapid.inspektør.sisteMelding("behandlingsresultat")
}
