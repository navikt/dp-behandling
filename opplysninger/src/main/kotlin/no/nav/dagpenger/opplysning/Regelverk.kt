package no.nav.dagpenger.opplysning

import no.nav.dagpenger.dag.DAG
import no.nav.dagpenger.dag.Edge
import no.nav.dagpenger.dag.Node
import no.nav.dagpenger.opplysning.dsl.VedtakBygger
import java.time.LocalDate

class Regelverk<T : Regelverkstype>(
    vararg regelsett: Regelsett<T>,
) {
    private val produsent = regelsett.flatMap { rs -> rs.produserer.map { it to rs } }.toMap()
    val produserer = regelsett.flatMap { it.produserer }.toSet()
    val regelsett = regelsett.toList()

    internal fun reglerFor(
        opplysningstype: Opplysningstype<*>,
        forDato: LocalDate = LocalDate.MIN,
    ): List<Any> = regelsettFor(opplysningstype).flatMap { it.regler(forDato) }

    fun regelsettAvType(type: RegelsettType) = regelsett.filter { it.type == type }

    fun regelsettFor(opplysningstype: Opplysningstype<*>): Set<Regelsett<T>> {
        val nødvendigeRegelsett = mutableSetOf<Regelsett<T>>()

        traverseOpplysningstyper(opplysningstype) { regelsett ->
            nødvendigeRegelsett.add(regelsett)
        }

        return nødvendigeRegelsett.toSet()
    }

    fun regeltreFor(opplysningstype: Opplysningstype<*>): DAG<Regelsett<T>, String> {
        val edges = mutableSetOf<Edge<Regelsett<T>, String>>()

        traverseOpplysningstyper(opplysningstype) { currentRegelsett ->
            for (avhengighet in currentRegelsett.avhengerAv) {
                val til = produsent[avhengighet] ?: continue
                edges.add(Edge(Node(currentRegelsett.navn, currentRegelsett), Node(til.navn, til), "avhenger av"))
            }
        }

        return DAG(edges.toList())
    }

    fun relevanteVilkår(opplysninger: LesbarOpplysninger): List<Regelsett<T>> =
        regelsett
            .filter { it.type == RegelsettType.Vilkår }
            .filter { it.skalKjøres(opplysninger) }
            .filter { it.påvirkerResultat(opplysninger) }

    // Bruker Breadth-First Search (BFS) til å traversere regelsettene
    private fun traverseOpplysningstyper(
        start: Opplysningstype<*>,
        block: (Regelsett<T>) -> Unit,
    ) {
        val visited = mutableSetOf<Opplysningstype<*>>()
        val queue = ArrayDeque<Opplysningstype<*>>()
        queue.add(start)

        while (queue.isNotEmpty()) {
            val gjeldendeOpplysningstype = queue.removeFirst()
            val produseresAv = produsent[gjeldendeOpplysningstype] ?: continue

            if (visited.add(gjeldendeOpplysningstype)) {
                block(produseresAv)
                queue.addAll(produseresAv.avhengerAv)
            }
        }
    }

    fun somVedtak(metadata: NoeGøyFraBehandlingen): Vedtak<T> = VedtakBygger<T>(metadata).bygg(this)
}
