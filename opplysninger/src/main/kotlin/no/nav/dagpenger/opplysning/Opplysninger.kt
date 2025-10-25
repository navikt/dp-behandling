package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter
import no.nav.dagpenger.opplysning.Opplysning.Companion.gyldigeFor
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.util.UUID

class Opplysninger private constructor(
    override val id: UUID,
    initielleOpplysninger: List<Opplysning<*>>,
    basertPå: Opplysninger? = null,
) : LesbarOpplysninger {
    constructor() : this(UUIDv7.ny(), emptyList(), null)
    private constructor(id: UUID, opplysninger: List<Opplysning<*>>) : this(id, opplysninger, null)

    private val egne: MutableList<Opplysning<*>> = initielleOpplysninger.toMutableList()
    private val fjernet: MutableList<Opplysning<*>> = mutableListOf()
    private val erstattet: MutableSet<UUID> get() = alleOpplysninger.mapNotNull { it.erstatter }.map { it.id }.toMutableSet()

    private val basertPåOpplysninger: List<Opplysning<*>> =
        basertPå?.let { it.basertPåOpplysninger + it.egne } ?: emptyList()

    private val alleOpplysninger = CachedList { (basertPåOpplysninger + egne).utenErstattet() }

    override val kunEgne get() = Opplysninger(id = id, opplysninger = egne)

    fun <T : Comparable<T>> leggTil(opplysning: Opplysning<T>) {
        val eksisterende = finnNullableOpplysning(opplysning.opplysningstype, opplysning.gyldighetsperiode)

        if (eksisterende != null) {
            if (egne.contains(eksisterende)) {
                // Erstatt hele opplysningen
                fjern(eksisterende)

                // Om den eksisterende opplysningen erstatter noe, så må den nye også erstatte den samme
                eksisterende.erstatter?.let {
                    opplysning.erstatter(it)
                    markerUtdatert(eksisterende)
                }
            }

            if (basertPåOpplysninger.contains(eksisterende)) {
                opplysning.erstatter(eksisterende)
                markerUtdatert(eksisterende)
            }
        }

        egne.add(opplysning)
        alleOpplysninger.refresh()
    }

    override fun erErstattet(opplysning: Opplysning<*>) = opplysning.id in erstattet

    internal fun <T : Comparable<T>> leggTilUtledet(opplysning: Opplysning<T>) = leggTil(opplysning)

    override fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> =
        finnNullableOpplysning(opplysningstype) ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig")

    override fun finnOpplysning(opplysningId: UUID) =
        alleOpplysninger.lastOrNull { it.id == opplysningId }
            ?: throw OpplysningIkkeFunnetException("Har ikke opplysning med id=$opplysningId")

    override fun har(opplysningstype: Opplysningstype<*>) = alleOpplysninger.any { it.er(opplysningstype) }

    override fun finnFlere(opplysningstyper: List<Opplysningstype<*>>) =
        opplysningstyper.mapNotNull { type -> alleOpplysninger.lastOrNull { it.er(type) } }

    override fun <T : Comparable<T>> finnAlle(opplysningstyper: List<Opplysningstype<T>>) =
        opplysningstyper.flatMap { type -> finnAlle(type) }

    override fun <T : Comparable<T>> finnAlle(opplysningstype: Opplysningstype<T>) =
        alleOpplysninger.filter { it.er(opplysningstype) }.filterIsInstance<Opplysning<T>>()

    override fun forDato(gjelderFor: LocalDate): LesbarOpplysninger {
        val forDato = alleOpplysninger.gyldigeFor(gjelderFor)
        val (aktiveForDato, basertPåDato) = forDato.partition { it in egne }
        return Opplysninger(id, aktiveForDato, Opplysninger(UUIDv7.ny(), basertPåDato))
    }

    override fun somListe(filter: Filter) =
        when (filter) {
            Filter.Alle -> alleOpplysninger
            Filter.Egne -> egne
        }.utenErstattet()

    fun baserPå(tidligereOpplysninger: Opplysninger?) = Opplysninger(id, egne, tidligereOpplysninger)

    fun fjernet(): Set<Opplysning<*>> = fjernet.toSet()

    fun fjernHvis(block: (Opplysning<*>) -> Boolean) =
        egne.filter { block(it) }.forEach { fjern(it, false) }.also {
            // Oppdaterer alleOpplysninger etter at opplysninger er fjernet
            alleOpplysninger.refresh()
        }

    fun fjern(opplysningId: UUID) = fjern(kunEgne.finnOpplysning(opplysningId))

    private fun fjern(
        opplysning: Opplysning<*>,
        skalOppfriske: Boolean = true,
    ) {
        // Fjern alle opplysninger som er utledet av opplysningen som fjernes
        fjernAvhengigheter(opplysning)

        if (egne.remove(opplysning)) {
            fjernet.add(opplysning)
        }

        if (skalOppfriske) alleOpplysninger.refresh()
    }

    private fun fjernAvhengigheter(eksisterende: Opplysning<*>) {
        val graf = OpplysningGraf(egne.toList())
        val avhengigheter = graf.hentAlleUtledetAv(eksisterende)
        avhengigheter.forEach { avhengighet -> fjern(avhengighet, false) }
    }

    private fun markerUtdatert(fra: Opplysning<*>) {
        val utledninger = alleOpplysninger.filter { it.utledetAv?.opplysninger?.contains(fra) ?: false }
        if (utledninger.isEmpty()) return

        fra.erUtdatert = true
        utledninger.forEach { markerUtdatert(it) }
    }

    private fun <T : Comparable<T>> finnNullableOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
    ): Opplysning<T>? {
        val opplysninger =
            alleOpplysninger
                .filter { it.er(opplysningstype) && it.gyldighetsperiode.overlapp(gyldighetsperiode) }
                .filterIsInstance<Opplysning<T>>()

        return opplysninger.lastOrNull()
    }

    private fun Collection<Opplysning<*>>.utenErstattet(): List<Opplysning<*>> {
        val bearbeidet =
            this
                .groupBy { it.opplysningstype }
                .mapValues { (_, perioder) ->
                    perioder
                        // Finn den siste for hver opplysning som har lik gyldighetsperiode
                        .distinctByLast<Opplysning<*>, Gyldighetsperiode> { it.gyldighetsperiode }
                        // Legg opplysninger som overlapper kant-i-kant hvor siste vinner
                        .zipWithNext()
                        .mapNotNull { (venstre, høyre) ->
                            if (!venstre.gyldighetsperiode.overlapp(høyre.gyldighetsperiode)) return@mapNotNull venstre
                            if (venstre.gyldighetsperiode.fraOgMed.isEqual(høyre.gyldighetsperiode.fraOgMed)) return@mapNotNull null
                            venstre.lagForkortet(høyre)
                        }
                        // Legg til den siste som ikke blir med i zipWithNext
                        .plus(perioder.last())
                        .toMutableList()
                }

        // Sorter opplysningene i samme rekkefølge som de var i før bearbeiding
        return this.mapNotNull { opplysning ->
            bearbeidet[opplysning.opplysningstype]?.takeIf { it.isNotEmpty() }?.removeFirst()
        }
    }

    companion object {
        fun med(opplysninger: Collection<Opplysning<*>>) = Opplysninger(UUIDv7.ny(), opplysninger.toList())

        fun med(vararg opplysning: Opplysning<*>) = Opplysninger(UUIDv7.ny(), opplysning.toList())

        fun basertPå(andre: Opplysninger) = Opplysninger(UUIDv7.ny(), emptyList(), andre)

        fun rehydrer(
            id: UUID,
            opplysninger: List<Opplysning<*>>,
        ) = Opplysninger(id, opplysninger, null)
    }
}

class OpplysningIkkeFunnetException(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)

class DuplikateOpplysningerException(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)

private inline fun <T, K> Iterable<T>.distinctByLast(selector: (T) -> K): List<T> {
    val map = LinkedHashMap<K, T>()
    for (element in this) {
        map[selector(element)] = element // overskriver hvis nøkkelen finnes fra før
    }
    return map.values.toList()
}
