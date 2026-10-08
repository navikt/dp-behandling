package no.nav.dagpenger.opplysning

import no.nav.dagpenger.dag.DAG
import no.nav.dagpenger.dag.Edge
import no.nav.dagpenger.dag.Node
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.util.UUID

@JvmInline
value class RegelverkIdent(
    val ident: String,
) {
    override fun toString() = ident
}

sealed class Avgjørelse {
    data object Innvilgelse : Avgjørelse() {
        override fun toString() = "Innvilgelse"
    }

    data object Avslag : Avgjørelse() {
        override fun toString() = "Avslag"
    }

    data object Stans : Avgjørelse() {
        override fun toString() = "Stans"
    }

    data object Gjenopptak : Avgjørelse() {
        override fun toString() = "Gjenopptak"
    }

    data object Endring : Avgjørelse() {
        override fun toString() = "Endring"
    }

    data object Uavklart : Avgjørelse() {
        override fun toString() = "Uavklart"
    }
}

fun interface Rettighetsperiodeberegning {
    fun rettighetsperioder(opplysninger: LesbarOpplysninger): List<Rettighetsperiode>
}

fun interface Utbetalingsberegning {
    fun utbetalinger(opplysninger: LesbarOpplysninger): List<Utbetaling>
}

fun interface Avgjørelsesberegning {
    fun avgjørelse(opplysninger: LesbarOpplysninger): Avgjørelse
}

class Regelverk(
    val ident: RegelverkIdent,
    private val rettighetsperiodeberegning: Rettighetsperiodeberegning = Rettighetsperiodeberegning { emptyList() },
    private val utbetalingsberegning: Utbetalingsberegning = Utbetalingsberegning { emptyList() },
    private val avgjørelsesberegning: Avgjørelsesberegning = Avgjørelsesberegning { Avgjørelse.Uavklart },
    vararg regelsett: Regelsett,
) {
    private val produsent = regelsett.flatMap { rs -> rs.produserer.map { it to rs } }.toMap()
    val produserer = regelsett.flatMap { it.produserer }.toSet()
    val regelsett = regelsett.toList()

    internal fun reglerFor(
        opplysningstype: Opplysningstype<*>,
        forDato: LocalDate = LocalDate.MIN,
    ): List<Any> = regelsettFor(opplysningstype).flatMap { it.regler(forDato) }

    fun kvoter() = regelsett.flatMap { it.kvoter }

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
            .filter { it.påvirkerResultat(opplysninger) }

    fun relevanteFastsettelser(opplysninger: LesbarOpplysninger): List<Regelsett> =
        regelsett
            .filter { it.type == RegelsettType.Fastsettelse }
            .filter { it.påvirkerResultat(opplysninger) }

    fun rettighetsperioder(opplysninger: LesbarOpplysninger) = rettighetsperiodeberegning.rettighetsperioder(opplysninger)

    fun utbetalinger(opplysninger: LesbarOpplysninger) = utbetalingsberegning.utbetalinger(opplysninger)

    fun avgjørelse(opplysninger: LesbarOpplysninger) = avgjørelsesberegning.avgjørelse(opplysninger)

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
    // harRett-verdien til perioden denne erstatter, om noen (null om perioden ikke erstatter noe).
    // Brukes til to formål:
    // 1) Skille en reell (men kalendermessig skjult) gjenopptak fra en ren videreføring, når perioden
    //    opphever en tidligere kjent stans (erstatterHarRett == false) - se harReeltOppholdEtter.
    // 2) Avgjøre forrige tilstand når en tidligere periode er fullstendig omgjort/restatert i samme
    //    behandling (samme fraOgMed, men f.eks. kortere varighet) - da overlever ingen arvet (uendret)
    //    periode å sammenligne med.
    val erstatterHarRett: Boolean? = null,
    // Id-en til opplysningen (f.eks. harLøpendeRett) perioden stammer fra. Overlever uendret selv om
    // opplysningen blir forkortet (se Opplysninger.forkortetTil, som bruker medGyldighetsperiode) eller
    // arvet uendret inn i en senere behandling i samme kjede - og kan derfor brukes som en stabil nøkkel
    // for å spore hvilken behandling som opprinnelig skapte en periode (se Rettighetstidslinje).
    val opplysningId: UUID = UUIDv7.ny(),
) : Comparable<Rettighetsperiode> {
    override fun compareTo(other: Rettighetsperiode): Int = fraOgMed.compareTo(other.fraOgMed)
}

data class Utbetaling(
    val meldeperiode: String,
    val dato: LocalDate,
    val sats: Int,
    val utbetaling: Int,
    val endret: Boolean,
    val ytelsestype: Ytelsestype,
)

@JvmInline
value class Ytelsestype(
    val navn: String,
)
