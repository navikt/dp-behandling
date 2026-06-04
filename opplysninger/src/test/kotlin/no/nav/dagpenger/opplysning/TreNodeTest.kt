package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TreNodeTest {
    val a6 = TreNode("a6")
    val a7 = TreNode("a7")
    val a5 = TreNode("a5", listOf(a6, a7))
    val a4 = TreNode("a4")
    val a3 = TreNode("a3")
    val a2 = TreNode("a2", listOf(a3))
    val a1 = TreNode("a1", listOf(a4, a5))
    private val tre = TreNode("Rot", listOf(a1, a2))

    @Test
    fun topologisk() {
        tre.topologisk() shouldBe listOf(a6, a7, a4, a5, a3, a1, a2, tre)
    }
}
