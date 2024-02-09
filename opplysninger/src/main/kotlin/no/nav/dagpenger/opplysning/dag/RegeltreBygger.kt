package no.nav.dagpenger.opplysning.dag

import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelsett
import no.nav.dagpenger.opplysning.regel.Regel

class RegeltreBygger(private val regler: List<Regel<*>>) {
    constructor(regelsett: Regelsett) : this(regelsett.regler())
    constructor(vararg regelsett: Regelsett) : this(regelsett.flatMap { it.regler() })

    fun dag(): DAG<Opplysningstype<*>> {
        val edges = regler.flatMap { edge(it) }
        return DAG(edges.toList())
    }

    private fun edge(regel: Regel<*>): List<Edge<Opplysningstype<*>>> {
        val currentNode = node(regel.produserer)

        return regel.avhengerAv.map { dependency ->
            val dependencyNode = node(dependency)
            Edge(from = currentNode, to = dependencyNode, edgeName = regel.javaClass.simpleName)
        }
    }

    private fun node(opplysningstype: Opplysningstype<*>) = Node(opplysningstype.navn, opplysningstype)
}
