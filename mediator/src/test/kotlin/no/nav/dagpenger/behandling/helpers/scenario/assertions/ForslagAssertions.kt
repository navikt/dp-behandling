package no.nav.dagpenger.behandling.helpers.scenario.assertions

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class ForslagAssertions(
    private val forslag: JsonNode,
) {
    val utfall = forslag["fastsatt"]["utfall"].asBoolean()

    fun medFastsettelser(block: Fastsettelser.() -> Unit) {
        Fastsettelser(forslag["fastsatt"]).apply { block() }
    }

    class Fastsettelser(
        private val jsonNode: JsonNode,
    ) {
        private val utfall = jsonNode["utfall"].asBoolean()

        val status get() = jsonNode["status"].asText()

        val grunnlag get() = jsonNode["grunnlag"]["grunnlag"].asInt()
        val vanligArbeidstidPerUke get() = jsonNode["fastsattVanligArbeidstid"]["vanligArbeidstidPerUke"].asDouble()
        val sats get() = jsonNode["sats"]["dagsatsMedBarnetillegg"].asInt()

        fun periode(navn: String) = jsonNode["kvoter"].find { it["navn"].asText() == navn }?.get("verdi")?.asInt()

        val samordning get() = jsonNode["samordning"]

        val oppfylt get() = withClue("Utfall skal være true") { utfall shouldBe true }

        val `ikke oppfylt` get() = withClue("Utfall skal være false") { utfall shouldBe false }
    }
}
