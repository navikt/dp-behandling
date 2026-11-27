package no.nav.dagpenger.dag.printer

import no.nav.dagpenger.dag.DAG
import no.nav.dagpenger.dag.Edge
import no.nav.dagpenger.dag.Node

/**
 * Skriver en DAG som GraphML, som kan åpnes i Gephi, yEd, Neo4j (apoc.import.graphml), ArcadeDB og lignende.
 * Kantene peker fra `from` til `to`, likt som i MermaidPrinter.
 *
 * Nodeegenskapen `label` er nodens navn. Kantegenskapen `label` er relasjonstypen (leses slik av APOC).
 * Egenskapen `labels` på noder (f.eks. `:Vilkår:Dato`) blir node-labels i Neo4j når `readLabels` er på.
 */
class GraphMLPrinter<T, E>(
    private val dag: DAG<T, E>,
    private val nodeEgenskaper: (Node<T>) -> Map<String, String?> = { emptyMap() },
    private val kantEgenskaper: (Edge<T, E>) -> Map<String, String?> = { mapOf("label" to it.edgeName) },
) : DAGPrinter {
    override fun toPrint(block: RootNodeFinner?): String {
        require(block == null) { "GraphMLPrinter does not support root node" }

        val ids = dag.nodes.withIndex().associate { (index, node) -> node to "n$index" }
        val noder = dag.nodes.map { it to egenskaper(mapOf("label" to it.name) + nodeEgenskaper(it)) }
        val kanter = dag.edges.map { it to egenskaper(kantEgenskaper(it)) }

        val nodeNøkler = noder.flatMap { it.second.keys }.toSortedSet()
        val kantNøkler = kanter.flatMap { it.second.keys }.toSortedSet()

        return buildList {
            add("""<?xml version="1.0" encoding="UTF-8"?>""")
            add("""<graphml xmlns="http://graphml.graphdrawing.org/xmlns">""")
            (nodeNøkler + kantNøkler).forEach { navn ->
                val gjelder =
                    when {
                        navn in nodeNøkler && navn in kantNøkler -> "all"
                        navn in nodeNøkler -> "node"
                        else -> "edge"
                    }
                add("""  <key id="${navn.xml()}" for="$gjelder" attr.name="${navn.xml()}" attr.type="string"/>""")
            }
            add("""  <graph id="G" edgedefault="directed">""")
            noder.forEach { (node, verdier) ->
                add("""    <node id="${ids.getValue(node)}">${data(verdier)}</node>""")
            }
            kanter.forEachIndexed { index, (kant, verdier) ->
                val kilde = ids.getValue(kant.from)
                val mål = ids.getValue(kant.to)
                add("""    <edge id="e$index" source="$kilde" target="$mål">${data(verdier)}</edge>""")
            }
            add("  </graph>")
            add("</graphml>")
        }.joinToString("\n")
    }

    private fun egenskaper(verdier: Map<String, String?>) = verdier.mapNotNull { (k, v) -> v?.let { k to it } }.toMap()

    private fun data(verdier: Map<String, String>) =
        verdier.entries.joinToString("") { (nøkkel, verdi) -> """<data key="${nøkkel.xml()}">${verdi.xml()}</data>""" }

    private fun String.xml() =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
