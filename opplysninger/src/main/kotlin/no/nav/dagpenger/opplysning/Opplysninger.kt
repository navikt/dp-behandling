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
    basertPå: List<Opplysninger>,
) : LesbarOpplysninger {
    constructor() : this(UUIDv7.ny(), emptyList(), emptyList())
    private constructor(id: UUID, opplysninger: List<Opplysning<*>>) : this(id, opplysninger, emptyList())

    private val egne: MutableList<Opplysning<*>> = initielleOpplysninger.toMutableList()
    private val fjernet: MutableList<Opplysning<*>> = mutableListOf()
    private val erstattet: MutableSet<UUID> = egne.mapNotNull { it.erstatter }.map { it.id }.toMutableSet()

    private val basertPåOpplysninger: List<Opplysning<*>> =
        basertPå.flatMap { it.basertPåOpplysninger.utenErstattet() + it.egne.utenErstattet() }

    private val alleOpplysninger = CachedList { basertPåOpplysninger.utenErstattet() + egne }

    override val kunEgne get() = Opplysninger(id = id, opplysninger = egne)

    fun <T : Comparable<T>> leggTil(opplysning: Opplysning<T>) {
        val eksisterende = finnNullableOpplysning(opplysning.opplysningstype, opplysning.gyldighetsperiode)

        if (eksisterende == null) {
            egne.add(opplysning)
            alleOpplysninger.refresh()
            return
        }

        if (basertPåOpplysninger.contains(eksisterende)) {
            // require(!opplysning.gyldighetsperiode.erUendelig) {
            // "Kan ikke legge til opplysning som har uendelig gyldighetsperiode når opplysningen finnes fra tidligere opplysninger"
            // }
            // Endre gyldighetsperiode på gammel opplysning og legg til ny opplysning kant i kant
            val erstattes: Opplysning<T>? = alleOpplysninger.filterIsInstance<Opplysning<T>>().find { it.overlapper(opplysning) }
            if (erstattes !== null) {
                when {
                    opplysning.overlapperHalenAv(erstattes) -> {
                        // Overlapp på halen av eksisterende opplysning
                        val forkortet = erstattes.lagForkortet(opplysning)
                        forkortet.erstatter(erstattes)
                        opplysning.erstatter(erstattes)
                        egne.add(forkortet)
                        egne.add(opplysning.nyID())
                    }

                    erstattes.harSammegyldighetsperiode(opplysning) -> {
                        // Overlapp for samme periode
                        opplysning.erstatter(erstattes)
                        egne.add(opplysning)
                    }

                    opplysning.starterFørOgOverlapper(erstattes) -> {
                        // Overlapp på starten av eksisterende opplysning
                        opplysning.erstatter(erstattes)
                        egne.add(opplysning)
                    }

                    // Forkortet gyldighetsperiode på eksisterende opplysning

                    erstattes.gyldighetsperiode.erUendelig -> {
                        // Opplysningen som erstattes har uendelig gyldighetsperiode
                        opplysning.erstatter(erstattes)
                        egne.add(opplysning)
                    }

                    else -> {
                        throw IllegalArgumentException(
                            """
                            |Kan ikke legge til opplysning (id=${opplysning.id}, type=${opplysning.opplysningstype.navn}) som 
                            |overlapper med eksisterende opplysning (id=${erstattes.id}, type=${erstattes.opplysningstype.navn}).
                            |gyldighetsperiode ny=${opplysning.gyldighetsperiode}, gammel=${erstattes.gyldighetsperiode}).
                            """.trimMargin(),
                        )
                    }
                }

                erstattet.add(erstattes.id)
                alleOpplysninger.refresh()
                return
            }
        }

        if (egne.contains(eksisterende)) {
            // Erstatt hele opplysningen
            fjern(eksisterende)

            // Om den eksisterende opplysningen erstatter noe, så må den nye også erstatte den samme
            eksisterende.erstatter?.let { opplysning.erstatter(it) }

            egne.add(opplysning)
            alleOpplysninger.refresh()
            return
        }

        throw IllegalStateException("Kan ikke legge til opplysning")
    }

    override fun erErstattet(opplysninger: List<Opplysning<*>>) = opplysninger.any { it.id in erstattet }

    internal fun <T : Comparable<T>> leggTilUtledet(opplysning: Opplysning<T>) = leggTil(opplysning)

    override fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> =
        finnNullableOpplysning(opplysningstype) ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig")

    override fun finnOpplysning(opplysningId: UUID) =
        alleOpplysninger.singleOrNull { it.id == opplysningId }
            ?: throw OpplysningIkkeFunnetException("Har ikke opplysning med id=$opplysningId")

    override fun har(opplysningstype: Opplysningstype<*>) = alleOpplysninger.any { it.er(opplysningstype) }

    override fun finnFlere(opplysningstyper: List<Opplysningstype<*>>) =
        opplysningstyper.mapNotNull { type -> alleOpplysninger.singleOrNull { it.er(type) } }

    override fun <T : Comparable<T>> finnAlle(opplysningstyper: List<Opplysningstype<T>>) =
        opplysningstyper.flatMap { type -> finnAlle(type) }

    override fun <T : Comparable<T>> finnAlle(opplysningstype: Opplysningstype<T>) =
        alleOpplysninger.filter { it.er(opplysningstype) }.filterIsInstance<Opplysning<T>>()

    override fun forDato(gjelderFor: LocalDate): LesbarOpplysninger {
        val aktiveForDato = egne.gyldigeFor(gjelderFor)
        // basertPåOpplysninger blir bare filtrert på init, men nye opplysninger kan ha blitt lagt til som nå erstatter noe og de må fjernes
        val basertPåDato = basertPåOpplysninger.utenErstattet().gyldigeFor(gjelderFor)
        return Opplysninger(id, aktiveForDato, listOf(Opplysninger(UUIDv7.ny(), basertPåDato)))
    }

    override fun somListe(filter: Filter) =
        when (filter) {
            Filter.Alle -> alleOpplysninger
            Filter.Egne -> egne
        }

    fun baserPå(tidligereOpplysninger: List<Opplysninger>) = Opplysninger(id, egne, tidligereOpplysninger)

    fun fjernet(): Set<Opplysning<*>> = fjernet.toSet()

    fun fjernHvis(block: (Opplysning<*>) -> Boolean) =
        egne.filter { block(it) }.forEach { fjern(it, false) }.also {
            // Oppdaterer alleOpplysninger etter at opplysninger er fjernet
            alleOpplysninger.refresh()
        }

    fun fjern(opplysningId: UUID) = fjern(finnOpplysning(opplysningId))

    private fun fjern(
        opplysning: Opplysning<*>,
        skalOppfriske: Boolean = true,
    ) {
        // Fjern alle opplysninger som er utledet av opplysningen som fjernes
        fjernAvhengigheter(opplysning)

        egne.remove(opplysning)
        fjernet.add(opplysning)

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

        require(opplysninger.size <= 1) {
            """Har mer enn 1 opplysning av type $opplysningstype i opplysningerId=$id.
            |Fant ${alleOpplysninger.count { it.er(opplysningstype) }} duplikater blant ${alleOpplysninger.size} opplysninger.
            |Basert på (${basertPåOpplysninger.size} opplysninger) 
            """.trimMargin()
        }

        return opplysninger.singleOrNull()
    }

    private fun Collection<Opplysning<*>>.utenErstattet(): List<Opplysning<*>> = filterNot { it.id in erstattet }

    companion object {
        fun med(opplysninger: Collection<Opplysning<*>>) = Opplysninger(UUIDv7.ny(), opplysninger.toList())

        fun med(vararg opplysning: Opplysning<*>) = Opplysninger(UUIDv7.ny(), opplysning.toList())

        fun basertPå(vararg andre: Opplysninger) = Opplysninger(UUIDv7.ny(), emptyList(), andre.toList())

        fun rehydrer(
            id: UUID,
            opplysninger: List<Opplysning<*>>,
        ) = Opplysninger(id, opplysninger, emptyList())
    }
}

class OpplysningIkkeFunnetException(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)
