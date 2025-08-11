package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.LesbarOpplysninger.Filter
import no.nav.dagpenger.opplysning.Opplysning.Companion.gyldigeFor
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.map

class Opplysninger private constructor(
    override val id: UUID,
    initielleOpplysninger: List<Opplysning<*>>,
    basertPå: Opplysninger? = null,
) : LesbarOpplysninger {
    constructor() : this(UUIDv7.ny(), emptyList(), null)
    private constructor(id: UUID, opplysninger: List<Opplysning<*>>) : this(id, opplysninger, null)

    private val egne: MutableList<Opplysning<*>> = initielleOpplysninger.toMutableList()
    private val fjernet: MutableList<Opplysning<*>> = mutableListOf()
    private val erstattet: MutableSet<UUID> = egne.mapNotNull { it.erstatter }.map { it.id }.toMutableSet()

    private val basertPåOpplysninger: List<Opplysning<*>> =
        basertPå?.let { it.basertPåOpplysninger.utenErstattet() + it.egne.utenErstattet() } ?: emptyList()

    private val alleOpplysninger = CachedList { basertPåOpplysninger.utenErstattet() + egne }

    override val kunEgne get() = Opplysninger(id = id, opplysninger = egne)

    fun <T : Comparable<T>> leggTil(opplysning: Opplysning<T>) {
        val eksisterende = finnNullableOpplysning(opplysning.opplysningstype, opplysning.gyldighetsperiode)

        if (eksisterende != null && egne.contains(eksisterende)) {
            // Erstatt hele opplysningen
            fjern(eksisterende)

            // Om den eksisterende opplysningen erstatter noe, så må den nye også erstatte den samme
            eksisterende.erstatter?.let { opplysning.erstatter(it) }
        }

        if (eksisterende != null && basertPåOpplysninger.contains(eksisterende)) {
            opplysning.erstatter(eksisterende)
        }

        egne.add(opplysning)
        alleOpplysninger.refresh()
    }

    override fun erErstattet(opplysninger: List<Opplysning<*>>) = opplysninger.any { it.id in erstattet }

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
        val aktiveForDato = egne.gyldigeFor(gjelderFor)
        // basertPåOpplysninger blir bare filtrert på init, men nye opplysninger kan ha blitt lagt til som nå erstatter noe og de må fjernes
        val basertPåDato = basertPåOpplysninger.utenErstattet().gyldigeFor(gjelderFor)
        return Opplysninger(id, aktiveForDato, Opplysninger(UUIDv7.ny(), basertPåDato))
    }

    override fun somListe(filter: Filter) =
        when (filter) {
            Filter.Alle -> alleOpplysninger
            Filter.Egne -> egne
        }

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

    private fun Collection<Opplysning<*>>.utenErstattet(): List<Opplysning<*>> = filterNot { it.id in erstattet }

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
