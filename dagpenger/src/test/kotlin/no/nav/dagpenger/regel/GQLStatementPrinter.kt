package no.nav.dagpenger.regel

import no.nav.dagpenger.dag.DAG
import no.nav.dagpenger.dag.printer.AlphabetIdGenerator
import no.nav.dagpenger.dag.printer.DAGPrinter
import no.nav.dagpenger.dag.printer.RootNodeFinner
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.regel.Regel

/**
 * Reifisert grafmodell der regler er egne noder med PRODUSERER/AVHENGER_AV-relasjoner.
 *
 * Grafstruktur:
 *   (Opplysning) <-[:PRODUSERER]- (Regel) -[:AVHENGER_AV]-> (Opplysning)
 */
internal class GrafPrinter(
    private val dag: DAG<Opplysningstype<*>, Regel<*>>,
    private val formatter: GrafFormatter = CypherFormatter,
) : DAGPrinter {
    override fun toPrint(block: RootNodeFinner?): String {
        require(block == null) { "GrafPrinter does not support root node" }

        val idGenerator = AlphabetIdGenerator()
        val opplysningIder = mutableMapOf<Opplysningstype<*>, String>()
        val regelIder = mutableMapOf<Regel<*>, String>()

        fun opplysningId(type: Opplysningstype<*>) = opplysningIder.getOrPut(type) { idGenerator.getNextId() }

        fun regelId(regel: Regel<*>) = regelIder.getOrPut(regel) { idGenerator.getNextId() }

        val statements = StringBuilder()

        // Opprett opplysningsnoder
        dag.nodes.forEach { node ->
            val type = node.data
            val id = opplysningId(type)
            statements.appendLine(
                formatter.opplysningsnode(
                    variabel = id,
                    uuid = type.id.uuid.toString(),
                    navn = type.navn,
                    datatype = type.datatype.navn(),
                    formaal = type.formål.name,
                    behovId = type.behovId,
                ),
            )
        }

        // Grupper edges per regel for å lage én regelnode per unik regel
        val reglerMedEdges = dag.edges.groupBy { it.data }

        reglerMedEdges.forEach { (regel, edges) ->
            if (regel == null) return@forEach
            val rId = regelId(regel)
            val regeltype = regel.javaClass.simpleName

            // Opprett regelnode
            statements.appendLine(
                formatter.regelnode(
                    variabel = rId,
                    type = regeltype,
                    beskrivelse = regel.toString(),
                ),
            )

            // PRODUSERER: Regel -> Opplysning den produserer
            val produsertType = edges.first().from.data
            statements.appendLine(
                formatter.relasjon(
                    fraVariabel = rId,
                    tilVariabel = opplysningId(produsertType),
                    type = "PRODUSERER",
                ),
            )

            // AVHENGER_AV: Regel -> Opplysninger den avhenger av
            edges.forEach { edge ->
                val avhengigType = edge.to.data
                statements.appendLine(
                    formatter.relasjon(
                        fraVariabel = rId,
                        tilVariabel = opplysningId(avhengigType),
                        type = "AVHENGER_AV",
                    ),
                )
            }
        }

        return statements.trim().toString()
    }
}

/** Abstraksjon for å formatere grafstatements i ulike syntakser. */
internal interface GrafFormatter {
    fun opplysningsnode(
        variabel: String,
        uuid: String,
        navn: String,
        datatype: String,
        formaal: String,
        behovId: String,
    ): String

    fun regelnode(
        variabel: String,
        type: String,
        beskrivelse: String,
    ): String

    fun relasjon(
        fraVariabel: String,
        tilVariabel: String,
        type: String,
    ): String
}

/** Neo4j Cypher-format */
internal data object CypherFormatter : GrafFormatter {
    override fun opplysningsnode(
        variabel: String,
        uuid: String,
        navn: String,
        datatype: String,
        formaal: String,
        behovId: String,
    ) = "CREATE ($variabel:Opplysning {id: '$uuid', navn: '${navn.escapeCypher()}', " +
        "datatype: '$datatype', formaal: '$formaal', behovId: '${behovId.escapeCypher()}'})"

    override fun regelnode(
        variabel: String,
        type: String,
        beskrivelse: String,
    ) = "CREATE ($variabel:Regel:$type {beskrivelse: '${beskrivelse.escapeCypher()}'})"

    override fun relasjon(
        fraVariabel: String,
        tilVariabel: String,
        type: String,
    ) = "CREATE ($fraVariabel)-[:$type]->($tilVariabel)"

    private fun String.escapeCypher() = replace("'", "\\'")
}

/** ISO GQL (ISO/IEC 39075) format */
internal data object GQLFormatter : GrafFormatter {
    override fun opplysningsnode(
        variabel: String,
        uuid: String,
        navn: String,
        datatype: String,
        formaal: String,
        behovId: String,
    ) = "INSERT ($variabel :Opplysning {id: '$uuid', navn: '${navn.escapeGQL()}', " +
        "datatype: '$datatype', formaal: '$formaal', behovId: '${behovId.escapeGQL()}'})"

    override fun regelnode(
        variabel: String,
        type: String,
        beskrivelse: String,
    ) = "INSERT ($variabel :Regel&$type {beskrivelse: '${beskrivelse.escapeGQL()}'})"

    override fun relasjon(
        fraVariabel: String,
        tilVariabel: String,
        type: String,
    ) = "INSERT ($fraVariabel)-[:$type]->($tilVariabel)"

    private fun String.escapeGQL() = replace("'", "\\'")
}
