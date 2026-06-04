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

    @Test
    fun bfs() {
        tre.bfs() shouldBe listOf(tre, a1, a2, a4, a5, a3, a6, a7)
    }

    @Test
    fun mermaid() {
        val expected =
            """
            flowchart BT 
                N_1((Rot))
                N_2((a1))
                N_3((a2))
                N_4((a4))
                N_5((a5))
                N_6((a3))
                N_7((a6))
                N_8((a7))
            
                N_1 --> N_2
                N_1 --> N_3
                N_2 --> N_4
                N_2 --> N_5
                N_3 --> N_6
            
                N_5 --> N_7
                N_5 --> N_8
            """.trimIndent()

        tre.mermaid() shouldBe expected
    }

    @Test
    fun `mermaid med like verdier i treet`() {
        val tre =
            TreNode(
                "roten",
                listOf(
                    TreNode("barn1", listOf(TreNode("felles"))),
                    TreNode("barn2", listOf(TreNode("felles"))),
                ),
            )
        val expected =
            """
            flowchart BT 
                N_1((roten))
                N_2((barn1))
                N_3((barn2))
                N_4((felles))
            
                N_1 --> N_2
                N_1 --> N_3
                N_2 --> N_4
                N_3 --> N_4
            """.trimIndent()

        tre.mermaid() shouldBe expected
    }
}
