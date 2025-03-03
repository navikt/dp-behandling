package no.nav.dagpenger.regel

import com.spun.util.persistence.Loader
import no.nav.dagpenger.dag.DAG
import no.nav.dagpenger.dag.printer.DAGPrinter
import no.nav.dagpenger.dag.printer.MermaidPrinter
import no.nav.dagpenger.dag.printer.RootNodeFinner
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelverk
import no.nav.dagpenger.opplysning.dag.RegeltreBygger
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class DiagramTest {
    private companion object {
        val path = "${
            Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")
        }/docs/"
        val options = Options().forFile().withExtension(".md")
    }

    @Test
    fun `printer hele dagpengeregeltreet`() {
        val bygger =
            RegeltreBygger(
                *RegelverkDagpenger.regelsett.toTypedArray(),
            )

        val regeltre = bygger.dag()
        val mermaidPrinter = MermaidPrinter(regeltre)
        val output = mermaidPrinter.toPrint()
        assertTrue(output.contains("graph RL"))

        println(output)

        @Language("Markdown")
        val markdown =
            """
                    ># Regeltre - Dagpenger (inngangsvilkår)
                    >
                    >## Regeltre
                    >
                    >```mermaid
                    >${output.trim()}
                    >```
                    """.trimMargin(">")
        skriv(
            markdown,
        )
    }

    @Test
    fun `lager tre av regelsettene`() {
        val regelverk =
            Regelverk(
                *RegelverkDagpenger.regelsett.toTypedArray(),
            )

        regelverk.regeltreFor(Minsteinntekt.minsteinntekt).also {
            val b = MermaidPrinter(it)
            println(b.toPrint())
        }
    }

    @Test
    fun `lager neo4j statements regelsettene`() {
        val bygger =
            RegeltreBygger(
                *RegelverkDagpenger.regelsett.toTypedArray(),
            )

        val regeltre = bygger.dag()
        val neo4jPrinter = Neo4jPrinter(regeltre)
        val output = neo4jPrinter.toPrint()
        println(output)
    }

    fun skriv(dokumentasjon: String) {
        Approvals.namerCreater = Loader { NamerWrapper({ "regeltre-dagpenger" }, { path }) }
        Approvals
            .verify(
                dokumentasjon,
                options,
            )
    }
}

class Neo4jPrinter(
    private val dag: DAG<*, *>,
) : DAGPrinter {
    override fun toPrint(block: RootNodeFinner?): String {
        require(block == null) { "Neo4jPrinter does not support root node" }

        val opp =
            dag.nodes.map {
                val type = it.data as Opplysningstype<*>

                """
                CREATE ({id: '${type.id.uuid}', navn: '${type.navn}'})
                """.trimIndent()
            }

        dag.edges.forEach { edge ->
            println(edge)

            // val fromNodeName = "$fromId[\"${edge.from.name}\"]"
            // val toNodeName = "$toId[\"${edge.to.name}\"]"
        }

        return opp.joinToString("\n")
    }
}
