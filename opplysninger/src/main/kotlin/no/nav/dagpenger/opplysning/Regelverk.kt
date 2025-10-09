package no.nav.dagpenger.opplysning

import no.nav.dagpenger.dag.DAG
import no.nav.dagpenger.dag.Edge
import no.nav.dagpenger.dag.Node
import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter.Egne
import java.time.LocalDate

data class Utfall(
    val virkningsdato: LocalDate,
    val utfall: Boolean,
)

class Regelverk(
    val rettighetsperiodetype: Opplysningstype<Boolean>? = null,
    vararg regelsett: Regelsett,
) {
    private val produsent = regelsett.flatMap { rs -> rs.produserer.map { it to rs } }.toMap()
    val produserer = regelsett.flatMap { it.produserer }.toSet()
    val regelsett = regelsett.toList()

    internal fun reglerFor(
        opplysningstype: Opplysningstype<*>,
        forDato: LocalDate = LocalDate.MIN,
    ): List<Any> = regelsettFor(opplysningstype).flatMap { it.regler(forDato) }

    fun regelsettAvType(type: RegelsettType) = regelsett.filter { it.type == type }

    fun regelsettFor(opplysningstype: Opplysningstype<*>): Set<Regelsett> {
        val nødvendigeRegelsett = mutableSetOf<Regelsett>()

        traverseOpplysningstyper(opplysningstype) { regelsett ->
            nødvendigeRegelsett.add(regelsett)
        }

        return nødvendigeRegelsett.toSet()
    }

    fun regeltreFor(opplysningstype: Opplysningstype<*>): DAG<Regelsett, String> {
        val edges = mutableSetOf<Edge<Regelsett, String>>()

        traverseOpplysningstyper(opplysningstype) { currentRegelsett ->
            for (avhengighet in currentRegelsett.avhengerAv) {
                val til = produsent[avhengighet] ?: continue
                edges.add(Edge(Node(currentRegelsett.navn, currentRegelsett), Node(til.navn, til), "avhenger av"))
            }
        }

        return DAG(edges.toList())
    }

    fun relevanteVilkår(opplysninger: LesbarOpplysninger): List<Regelsett> =
        regelsett
            .filter { it.type == RegelsettType.Vilkår }
            .filter { it.skalKjøres(opplysninger) }
            .filter { it.påvirkerResultat(opplysninger) }

    fun relevanteFastsettelser(opplysninger: LesbarOpplysninger): List<Regelsett> =
        regelsett
            .filter { it.type == RegelsettType.Fastsettelse }
            .filter { it.skalKjøres(opplysninger) }
            .filter { it.påvirkerResultat(opplysninger) }

    fun rettighetsperioder(opplysninger: LesbarOpplysninger): List<Rettighetsperiode> {
        if (rettighetsperiodetype == null) return emptyList()

        val egne = opplysninger.somListe(Egne)
        return opplysninger.finnAlle(rettighetsperiodetype).map { periode ->
            Rettighetsperiode(
                fraOgMed = periode.gyldighetsperiode.fraOgMed,
                tilOgMed = periode.gyldighetsperiode.tilOgMed,
                harRett = periode.verdi,
                endret = egne.contains(periode),
            )
        }
    }

    val vilkårsopplysninger by lazy {
        regelsett
            .filter { regelsett -> regelsett.type == RegelsettType.Vilkår }
            .flatMap { it.betingelser }
    }

    // Bruker Breadth-First Search (BFS) til å traversere regelsettene
    private fun traverseOpplysningstyper(
        start: Opplysningstype<*>,
        block: (Regelsett) -> Unit,
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
}

data class Rettighetsperiode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val harRett: Boolean,
    val endret: Boolean,
)
