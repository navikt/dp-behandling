package no.nav.dagpenger.regel

import no.nav.dagpenger.dag.DAG
import no.nav.dagpenger.dag.printer.AlphabetIdGenerator
import no.nav.dagpenger.dag.printer.DAGPrinter
import no.nav.dagpenger.dag.printer.RootNodeFinner
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel
import java.util.UUID

internal class GQLStatementPrinter(
    private val dag: DAG<Opplysningstype<*>, Regel<*>>,
) : DAGPrinter {
    override fun toPrint(block: RootNodeFinner?): String {
        require(block == null) { "Neo4jPrinter does not support root node" }
        val nodes = mutableMapOf<UUID, String>()
        val alphabetIdGenerator = AlphabetIdGenerator()
        val noder =
            dag.nodes.map {
                val type = it.data
                nodes[type.id.uuid] = alphabetIdGenerator.getNextId()

                """
                CREATE (${nodes[type.id.uuid]}:Opplysning{id: '${type.id.uuid}', navn: '${type.navn}'})
                """.trimIndent()
            }

        val relasjoner = StringBuilder()

        dag.edges.forEach { edge ->
            val fra = edge.from.data
            val til = edge.to.data
            val relasjon = edge.edgeName
            val beskrivelse = edge.data.toString()
            val idFra = nodes[fra.id.uuid]
            val idTil = nodes[til.id.uuid]
            val relationshipStatement =
                """
                CREATE ($idFra)-[:$relasjon {beskrivelse: '$beskrivelse', regel: '$relasjon'}]->($idTil) 
                """.trimIndent()

            relasjoner.appendLine(relationshipStatement)
        }

        return noder.joinToString("\n") + "\n" + relasjoner.toString()
    }
}
