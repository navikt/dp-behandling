package no.nav.dagpenger.opplysning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

internal class ProsesskontekstTest {
    @Test
    fun `kaster med årsaker når for mange runder har bedt om rekjøring`() {
        val kontekst = Prosesskontekst(Opplysninger())

        repeat(10) {
            kontekst.startNyRunde()
            kontekst.beOmRekjøring("PluginA")
        }
        kontekst.startNyRunde()
        kontekst.beOmRekjøring("PluginA")

        val feil = shouldThrow<IllegalStateException> { kontekst.startNyRunde() }
        feil.message shouldContain "Fanget i uendelig løkke"
        feil.message shouldContain "PluginA=11"
    }

    @Test
    fun `gjentatt rekjøringsårsak telles én gang per runde`() {
        val kontekst = Prosesskontekst(Opplysninger())

        kontekst.beOmRekjøring("PluginA")
        kontekst.beOmRekjøring("PluginA")

        kontekst.kreverRekjøring shouldBe true
        kontekst.antallRekjøringerRunder shouldBe 1
        kontekst.rekjøringerPerÅrsak shouldBe mapOf("PluginA" to 1)
    }

    @Test
    fun `ulike årsaker samme runde telles separat men utgjør én runde`() {
        val kontekst = Prosesskontekst(Opplysninger())

        kontekst.beOmRekjøring("PluginA")
        kontekst.beOmRekjøring("PluginB")

        kontekst.antallRekjøringerRunder shouldBe 1
        kontekst.rekjøringerPerÅrsak shouldBe mapOf("PluginA" to 1, "PluginB" to 1)
    }

    @Test
    fun `ny runde nullstiller rekjøringsflagget og beholder opptellingen`() {
        val kontekst = Prosesskontekst(Opplysninger())
        kontekst.beOmRekjøring("PluginA")

        kontekst.startNyRunde()

        kontekst.kreverRekjøring shouldBe false
        kontekst.antallRekjøringerRunder shouldBe 1
        kontekst.rekjøringerPerÅrsak shouldBe mapOf("PluginA" to 1)

        kontekst.startNyRunde()

        kontekst.kreverRekjøring shouldBe false
        kontekst.antallRekjøringerRunder shouldBe 1
        kontekst.rekjøringerPerÅrsak shouldBe mapOf("PluginA" to 1)
    }

    @Test
    fun `årsaker telles videre på tvers av runder`() {
        val kontekst = Prosesskontekst(Opplysninger())
        kontekst.beOmRekjøring("PluginA")
        kontekst.beOmRekjøring("PluginB")

        kontekst.startNyRunde()
        kontekst.beOmRekjøring("PluginA")
        kontekst.beOmRekjøring("PluginA")

        kontekst.kreverRekjøring shouldBe true
        kontekst.antallRekjøringerRunder shouldBe 2
        kontekst.rekjøringerPerÅrsak shouldBe mapOf("PluginA" to 2, "PluginB" to 1)
    }

    @Test
    fun `runder uten forespørsler teller ikke som rekjøringer`() {
        val kontekst = Prosesskontekst(Opplysninger())
        kontekst.startNyRunde()
        kontekst.startNyRunde()

        kontekst.kreverRekjøring shouldBe false
        kontekst.antallRekjøringerRunder shouldBe 0
        kontekst.rekjøringerPerÅrsak shouldBe emptyMap()

        kontekst.beOmRekjøring("PluginA")
        kontekst.startNyRunde()
        kontekst.startNyRunde()
        kontekst.beOmRekjøring("PluginA")

        kontekst.kreverRekjøring shouldBe true
        kontekst.antallRekjøringerRunder shouldBe 2
        kontekst.rekjøringerPerÅrsak shouldBe mapOf("PluginA" to 2)
    }

    @Test
    fun `ny kontekst starter med tom opptelling`() {
        val første = Prosesskontekst(Opplysninger())
        første.beOmRekjøring("PluginA")

        val ny = Prosesskontekst(Opplysninger())

        ny.kreverRekjøring shouldBe false
        ny.antallRekjøringerRunder shouldBe 0
        ny.rekjøringerPerÅrsak shouldBe emptyMap()
    }
}
