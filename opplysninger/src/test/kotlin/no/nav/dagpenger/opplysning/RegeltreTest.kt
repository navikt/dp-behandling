package no.nav.dagpenger.opplysning

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RegeltreTest {
    @Test
    fun foo() {
        val tre =
            Node(
                name = "Rot",
                children =
                    listOf(
                        Node(
                            name = "a1",
                            children =
                                listOf(
                                    Node(
                                        name = "a4",
                                    ),
                                    Node(
                                        name = "a5",
                                        children =
                                            listOf(
                                                Node(name = "a6"),
                                                Node(name = "a7"),
                                            ),
                                    ),
                                ),
                        ),
                        Node(
                            name = "a2",
                            children =
                                listOf(
                                    Node("a3"),
                                ),
                        ),
                    ),
            )

        tre.sti() shouldBe "Rot, a1, a2, a4, a5, a3, a6, a7"
        tre.stiRev() shouldBe "a6, a7, a4, a5, a3, a1, a2, Rot"
    }

    private data class Node(
        val name: String,
        val children: List<Node> = emptyList(),
    ) {
        fun breadthFirst(): List<String> {
            // gå gjennom regeltreet dybde først
            val kø = mutableListOf(Pair(this, emptyList<String>()))
            val rekkefølge = mutableListOf<String>()
            while (kø.isNotEmpty()) {
                val (n, sti) = kø.removeFirst()

                println("Hvis ${n.name} var dirty ville jeg planlagt følgende regler: ${(sti.plusElement(n.name)).joinToString(" -> ")}")

                rekkefølge.add(n.name)
                kø.addAll(n.children.map { Pair(it, sti.plusElement(n.name)) })
            }
            return rekkefølge
        }

        fun sti(): String = breadthFirst().joinToString(", ")

        fun stiRev(): String {
            // gå gjennom regeltreet dybde først
            val kø = mutableListOf(Pair(this, emptyList<String>()))
            val rekkefølge = mutableListOf<String>()
            while (kø.isNotEmpty()) {
                val (n, sti) = kø.removeFirst()

                println("Hvis ${n.name} var dirty ville jeg planlagt følgende regler: ${(sti.plusElement(n.name)).joinToString(" -> ")}")

                rekkefølge.addFirst(n.name)
                kø.addAll(n.children.reversed().map { Pair(it, sti.plusElement(n.name)) })
            }
            return rekkefølge.joinToString(", ")
        }
    }
}
