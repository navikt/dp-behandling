package no.nav.dagpenger.opplysning

data class TreNode<T>(
    val verdi: T,
    val avhengigheter: List<TreNode<T>> = emptyList(),
) {
    fun topologisk() = iterate(Ordering.TOPOLOGISK)

    fun bfs() = iterate(Ordering.BFS)

    private fun iterate(ordering: Ordering): List<TreNode<T>> {
        val kø = mutableListOf(this)
        val result = mutableListOf<TreNode<T>>()
        while (kø.isNotEmpty()) {
            val n = kø.removeFirst()
            when (ordering) {
                Ordering.TOPOLOGISK -> {
                    result.addFirst(n)
                    kø.addAll(n.avhengigheter.reversed())
                }

                Ordering.BFS -> {
                    result.add(n)
                    kø.addAll(n.avhengigheter)
                }
            }
        }
        return result
    }

    private enum class Ordering {
        TOPOLOGISK,
        BFS,
    }
}

internal fun <T> TreNode<T>.mermaid(): String {
    val nodes = bfs()
    val nodeId = nodes.associateWith { node -> "N_${nodes.indexOf(node) + 1}" }

    fun TreNode<T>.id(): String = nodeId.getValue(this)

    val indent = "    "

    // definer alle nodene først
    val definitions =
        nodes.joinToString("\n") {
            "$indent${it.id()}((${it.verdi}))"
        }
    val connections =
        nodes.joinToString("\n") { node ->
            node.avhengigheter.joinToString("\n") { avhengighet ->
                "$indent${node.id()} --> ${avhengighet.id()}"
            }
        }

    return """
        |flowchart BT 
        |$definitions
        |
        |$connections
        """.trimMargin().trimEnd()
}
