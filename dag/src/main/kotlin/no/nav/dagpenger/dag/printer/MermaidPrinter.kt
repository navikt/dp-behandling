package no.nav.dagpenger.dag.printer

import no.nav.dagpenger.dag.DAG

class MermaidPrinter(
    private val dag: DAG<*, *>,
    private val retning: String = "RL",
) : DAGPrinter {
    private val nodeIds = NodeIds()

    override fun toPrint(block: RootNodeFinner?): String {
        require(block == null) { "MermaidPrinter does not support root node" }

        val diagram = StringBuilder()
        diagram.appendLine("graph $retning")
        dag.edges.forEach { edge ->
            val fromId = nodeIds.id(edge.from)
            val toId = nodeIds.id(edge.to)

            val fromNodeName = "$fromId[\"${edge.from.name}\"]"
            val toNodeName = "$toId[\"${edge.to.name}\"]"

            diagram.appendLine("  $fromNodeName -->|\"${edge.edgeName}\"| $toNodeName")
        }

        return diagram.trim().toString()
    }
}

private class NodeIds(
    private val idGenerator: AlphabetIdGenerator = AlphabetIdGenerator(),
    private val ids: HashMap<Any, String> = hashMapOf(),
) {
    fun id(node: Any) = ids.computeIfAbsent(node) { nextId() }

    private fun nextId() = idGenerator.getNextId()
}
