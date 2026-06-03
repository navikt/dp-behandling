package no.nav.dagpenger.opplysning.regel

import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.Hypotese
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Opplysning
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.opplysning.Regelplanlegger
import no.nav.dagpenger.opplysning.Utledning
import java.time.LocalDate

typealias Regelkø = ArrayDeque<Regel<*>>

abstract class Regel<T : Any> internal constructor(
    internal val produserer: Opplysningstype<T>,
    // todo: Bør dette være et Set? Vi er ikke avhengig av rekkefølge
    internal val avhengerAv: List<Opplysningstype<out Any>> = emptyList(),
) {
    init {
        require(avhengerAv.none { it == produserer }) {
            "Regel ${this::class.java.simpleName} kan ikke produsere samme opplysning ${produserer.navn} den er avhengig av"
        }
    }

    /**
     * 1. bygge tre basert på avhengighetene
     * 2. gå gjennom tre og finn første opplysning som må produseres, enten fordi den mangler eller er utdatert
     */
    private data class Node(
        val regel: Regel<*>,
        val avhengigheter: List<Node>,
    ) {
        fun breadthFirstByDeepest(): List<Node> {
            // går gjennom treet breadth-first, men gjør slik at de dypeste nodene kommer først i listen
            val kø = mutableListOf(this)
            val breadthFirstByDeepest = mutableListOf<Node>()
            while (kø.isNotEmpty()) {
                val n = kø.removeFirst()
                breadthFirstByDeepest.addFirst(n)
                kø.addAll(n.avhengigheter.reversed())
            }
            return breadthFirstByDeepest
        }
    }

    private fun regeltre(produsenter: Map<Opplysningstype<out Any>, Regel<*>>): Node {
        if (avhengerAv.isEmpty()) return Node(this, emptyList())
        return Node(
            this,
            avhengerAv
                .map { produsenter[it] ?: error("har ikke produsent for $it") }
                .map { it.regeltre(produsenter) },
        )
    }

    private fun planForRegel(
        regeltre: Node,
        opplysninger: LesbarOpplysninger,
    ): Set<Regel<*>> {
        // vi besøker de dypeste løvnodene først (de reglene som ikke avhenger av noen),
        // før vi tar for oss nivå-for-nivå.
        // vi gjør dette fordi planen må spesifisere hvilke regler som må kjøres før de andre, slik
        // at opplysningene som produseres av disse reglene er tilgjengelige for reglene som kommer senere i planen.
        val planlegger = mutableSetOf<Regel<*>>()
        regeltre
            .breadthFirstByDeepest()
            .forEach { node ->
                val produkt = opplysninger.finnNullableOpplysning(node.regel.produserer)
                // sjekker ikke om regelen selv sin opplysning er utdatert 🤔
                val harUtdaterteAvhengigheter = produkt?.utledetAv?.opplysninger?.any { it.erUtdatert } == true
                // hvis en avhengighet tidligere i kjeden er planlagt skal vi også kjøre
                val avhengighetSkalKjøre = node.avhengigheter.any { it.regel in planlegger }

                if (produkt == null || harUtdaterteAvhengigheter || avhengighetSkalKjøre) {
                    // legger til baklengs fordi produktet brukes av reglene som kommer etterpå
                    planlegger.add(node.regel)
                }
            }
        return planlegger
    }

    internal fun lagPlan(
        opplysninger: LesbarOpplysninger,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
    ): Set<Regel<*>> {
        val regeltre = regeltre(produsenter)
        val planForRegel = planForRegel(regeltre, opplysninger)
        return planForRegel
    }

    // Beslutter om denne regelen skal legges til planen,
    // eller om avhengigheter skal legges i køen for videre evaluering.
    internal open fun lagPlan(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        kø: Regelkø,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
    ) {
        val produkt = opplysninger.finnNullableOpplysning(produserer)
        when {
            produkt == null -> {
                lagPlanNårProduktMangler(opplysninger, plan, kø, produsenter)
            }

            produkt.utledetAv != null -> {
                lagPlanFraUtledning(produkt.utledetAv, opplysninger, plan, kø, produsenter)
            }

            else -> {
                // opplysningen er ikke utledet av noe ELLER overstyrt av saksbehandler
            }
        }
    }

    private fun lagPlanNårProduktMangler(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        kø: Regelkø,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
    ) {
        val avhengigheter = opplysninger.finnFlere(avhengerAv)
        if (avhengigheter.size == avhengerAv.size) {
            // Alle avhengigheter er tilstede — legg denne regelen til planen
            plan.add(this)
        } else {
            // Minst én avhengighet mangler — kø inn produsentene
            avhengerAv.forEach { avhengighet ->
                val produsent = produsenter[avhengighet]
                if (produsent == null) {
                    manglendeRegler.add(avhengighet)
                    return@forEach
                }
                kø.add(produsent)
            }
        }
    }

    private fun lagPlanNårUtledereErUtdaterte(
        utdaterteOpplysninger: List<Opplysning<*>>,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
        kø: Regelkø,
    ) {
        // Minst en av opplysningene som regelen er avhengig av er utdatert, regelen skal kjøres på nytt
        utdaterteOpplysninger.forEach { utdatert ->
            val produsent =
                produsenter[utdatert.opplysningstype] ?: return
            kø.add(produsent)
        }
    }

    private fun lagPlanNårRegelenHarFåttNyeAvhengigheter(
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        kø: Regelkø,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
    ) {
        val mangler = avhengerAv.filter { opplysninger.mangler(it) }
        if (mangler.isEmpty()) return plan.add(this)
        mangler.forEach { avhengighet ->
            produsenter[avhengighet]?.let { kø.add(it) }
        }
    }

    private fun lagPlanFraUtledning(
        utledning: Utledning,
        opplysninger: LesbarOpplysninger,
        plan: Regelplanlegger,
        kø: Regelkø,
        produsenter: Map<Opplysningstype<out Any>, Regel<*>>,
    ) {
        val (erErstattet, ikkeErstattet) = utledning.opplysninger.partition { opplysninger.erErstattet(listOf(it)) }
        val utdaterteOpplysninger = ikkeErstattet.filter { it.erUtdatert }

        when {
            // Sjekk om produktet er basert på utdatert informasjon
            utdaterteOpplysninger.isNotEmpty() -> {
                lagPlanNårUtledereErUtdaterte(utdaterteOpplysninger, produsenter, kø)
            }

            // Sjekk om produktet er basert på erstattet informasjon
            erErstattet.isNotEmpty() -> {
                // Minst en avhengighet er erstattet, må de regelen skal kjøres på nytt
                plan.add(this)
            }

            // Sjekk om regelen har fått nye avhengigheter
            harRegelNyeAvhengigheter(utledning) -> {
                lagPlanNårRegelenHarFåttNyeAvhengigheter(opplysninger, plan, kø, produsenter)
            }
        }
    }

    private fun harRegelNyeAvhengigheter(utledetAv: Utledning) =
        avhengerAv.toSet() != utledetAv.opplysninger.map { it.opplysningstype }.toSet()

    abstract override fun toString(): String

    protected abstract fun kjør(opplysninger: LesbarOpplysninger): T

    fun produserer(opplysningstype: Opplysningstype<*>) = produserer.er(opplysningstype)

    internal fun lagProdukt(opplysninger: LesbarOpplysninger): Opplysning<T> {
        if (avhengerAv.isEmpty()) {
            val produkt = kjør(opplysninger)
            return Faktum(produserer, produkt)
        }

        val basertPå = opplysninger.finnFlere(avhengerAv)

        val produkt = kjør(opplysninger)
        val erAlleFaktum = basertPå.all { it is Faktum<*> }
        val utledetAv = Utledning(this, basertPå)
        val gyldighetsperiode = produserer.gyldighetsperiode(produkt, basertPå)

        return when (erAlleFaktum) {
            true -> Faktum(opplysningstype = produserer, verdi = produkt, utledetAv = utledetAv, gyldighetsperiode = gyldighetsperiode)
            false -> Hypotese(opplysningstype = produserer, verdi = produkt, utledetAv = utledetAv, gyldighetsperiode = gyldighetsperiode)
        }
    }
}

fun interface GyldighetsperiodeStrategi<T> {
    companion object {
        private val minsteMulige =
            GyldighetsperiodeStrategi<Any> { _, basertPå ->
                if (basertPå.isEmpty()) return@GyldighetsperiodeStrategi Gyldighetsperiode()
                Gyldighetsperiode(
                    fraOgMed = basertPå.maxOf { it.gyldighetsperiode.fraOgMed },
                    tilOgMed = basertPå.minOf { it.gyldighetsperiode.tilOgMed },
                )
            }
        val egenVerdi = GyldighetsperiodeStrategi<LocalDate> { produkt, _ -> Gyldighetsperiode(fom = produkt) }

        @Suppress("UNCHECKED_CAST")
        fun <P> minsteMulige() = minsteMulige as GyldighetsperiodeStrategi<P>

        fun <P> basertPå(opplysningstype: Opplysningstype<LocalDate>) =
            GyldighetsperiodeStrategi<P> { _, basertPå ->
                val dato = basertPå.single { it.opplysningstype == opplysningstype }.verdi
                require(dato is LocalDate) { "Opplysningstype som skal brukes til å utlede gyldighetsperiode må være LocalDate" }
                Gyldighetsperiode(dato)
            }
    }

    fun gyldighetsperiode(
        produkt: T,
        basertPå: List<Opplysning<*>>,
    ): Gyldighetsperiode
}

val manglendeRegler = mutableSetOf<Opplysningstype<*>>()
