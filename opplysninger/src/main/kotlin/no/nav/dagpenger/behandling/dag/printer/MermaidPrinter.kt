package no.nav.dagpenger.behandling.dag.printer

import no.nav.dagpenger.behandling.dag.DAG

class MermaidPrinter(private val dag: DAG<*>, private val retning: String = "RL") : DAGPrinter {
    override fun toPrint(block: RootNodeFinner?): String {
        require(block == null) { "MermaidPrinter does not support root node" }

        val diagram = StringBuilder()
        diagram.appendLine("graph $retning")
        dag.edges.forEach { edge ->
            val fromNodeName = "${edge.from.hashCode()}[\"${edge.from.name}\"]"
            val toNodeName = "${edge.to.hashCode()}[\"${edge.to.name}\"]"
            diagram.appendLine("  $fromNodeName -->|${edge.edgeName}| $toNodeName")
        }

        return diagram.trim().toString()
    }
}