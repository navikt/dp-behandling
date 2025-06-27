package no.nav.dagpenger.opplysning

import mu.KotlinLogging
import no.nav.dagpenger.opplysning.Opplysning.Companion.gyldigeFor
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.util.UUID
import kotlin.collections.map

class Opplysninger private constructor(
    override val id: UUID,
    initielleOpplysninger: List<Opplysning<*>> = emptyList(),
    basertPå: List<Opplysninger> = emptyList(),
) : LesbarOpplysninger {
    constructor() : this(UUIDv7.ny(), emptyList(), emptyList())
    constructor(id: UUID, opplysninger: List<Opplysning<*>>) : this(id, opplysninger, emptyList())
    constructor(opplysninger: List<Opplysning<*>>, basertPå: List<Opplysninger> = emptyList()) : this(UUIDv7.ny(), opplysninger, basertPå)
    constructor(vararg basertPå: Opplysninger) : this(emptyList(), basertPå.toList())

    private val egne: MutableList<Opplysning<*>> = initielleOpplysninger.toMutableList()
    private val fjernet: MutableList<Opplysning<*>> = mutableListOf()

    private val basertPåOpplysninger: List<Opplysning<*>> =
        basertPå
            .flatMap {
                it.basertPåOpplysninger.utenErstattet() + it.egne.utenErstattet()
            }

    private val alleOpplysninger = CachedList { basertPåOpplysninger.utenErstattet() + egne }

    // TODO: Denne burde bare brukes av databaselaget
    val aktiveOpplysningerListe get() = egne.toList()

    override val egneOpplysninger get() = Opplysninger(id = id, opplysninger = egne)

    override fun forDato(gjelderFor: LocalDate): LesbarOpplysninger {
        val aktiveForDato = aktiveOpplysningerListe.gyldigeFor(gjelderFor)
        val basertPåDato = basertPåOpplysninger.utenErstattet().gyldigeFor(gjelderFor)
        return Opplysninger(id, aktiveForDato, listOf(Opplysninger(UUIDv7.ny(), basertPåDato)))
    }

    override fun erErstattet(opplysninger: List<Opplysning<*>>): Boolean {
        val id = opplysninger.map { it.id }
        return opplysningerSomErErstattet.any { it in id }
    }

    @Suppress("UNCHECKED_CAST")
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
            val erstattes: Opplysning<T>? = alleOpplysninger.utenErstattet().find { it.overlapper(opplysning) } as Opplysning<T>?
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

        egne.add(opplysning)
        alleOpplysninger.refresh()
    }

    internal fun <T : Comparable<T>> leggTilUtledet(opplysning: Opplysning<T>) = leggTil(opplysning)

    override fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> =
        finnNullableOpplysning(opplysningstype) ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig")

    override fun finnOpplysning(opplysningId: UUID) =
        alleOpplysninger.singleOrNull { it.id == opplysningId }
            ?: throw OpplysningIkkeFunnetException("Har ikke opplysning med id=$opplysningId")

    override fun har(opplysningstype: Opplysningstype<*>) = alleOpplysninger.utenErstattet().any { it.er(opplysningstype) }

    override fun finnAlle(opplysningstyper: List<Opplysningstype<*>>) = finnAlle(*opplysningstyper.toTypedArray())

    override fun finnAlle(vararg opplysningstyper: Opplysningstype<*>) =
        opplysningstyper.flatMap { type -> alleOpplysninger.filter { it.er(type) } }

    override fun finnAlle() = alleOpplysninger.toList()

    fun fjernet(): Set<Opplysning<*>> = fjernet.toSet()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Comparable<T>> finnNullableOpplysning(
        opplysningstype: Opplysningstype<T>,
        gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
    ): Opplysning<T>? {
        if (alleOpplysninger.utenErstattet().count { it.er(opplysningstype) && it.gyldighetsperiode.overlapp(gyldighetsperiode) } > 1) {
            throw IllegalStateException(
                """Har mer enn 1 opplysning av type $opplysningstype i opplysningerId=$id.
                |Fant ${alleOpplysninger.count { it.er(opplysningstype) }} duplikater blant ${alleOpplysninger.size} opplysninger.
                |Basert på (${basertPåOpplysninger.size} opplysninger) 
                """.trimMargin(),
            )
        }
        return alleOpplysninger.utenErstattet().singleOrNull {
            it.er(opplysningstype) && it.gyldighetsperiode.overlapp(gyldighetsperiode)
        } as Opplysning<T>?
    }

    private fun <T : Comparable<T>> Opplysning<T>.overlapperHalenAv(opplysning: Opplysning<T>) =
        this.gyldighetsperiode.fom.isAfter(opplysning.gyldighetsperiode.fom) &&
            this.gyldighetsperiode.fom <= opplysning.gyldighetsperiode.tom

    private fun <T : Comparable<T>> Opplysning<T>.harSammegyldighetsperiode(opplysning: Opplysning<T>) =
        this.gyldighetsperiode == opplysning.gyldighetsperiode

    private fun <T : Comparable<T>> Opplysning<T>.starterFørOgOverlapper(opplysning: Opplysning<T>) =
        this.gyldighetsperiode.fom.isBefore(opplysning.gyldighetsperiode.fom) &&
            opplysning.gyldighetsperiode.inneholder(this.gyldighetsperiode.tom)

    operator fun plus(tidligereOpplysninger: List<Opplysninger>) = Opplysninger(id, egne, tidligereOpplysninger)

    fun fjernUbrukteOpplysninger(beholdDisse: Set<Opplysningstype<*>>) {
        egne
            .filterNot { beholdDisse.contains(it.opplysningstype) }
            .filterNot {
                (it.erstatter != null).also { erstatter ->
                    if (!erstatter) return@also
                    logger.warn {
                        """Prøver å fjerne opplysning id=${it.id}, navn=${it.opplysningstype.navn}, 
                        |som er en erstatning for id=${it.erstatter!!.id}
                        """.trimMargin()
                    }
                }
            }.forEach { fjern(it) }
        alleOpplysninger.refresh()
    }

    fun fjern(opplysningId: UUID) {
        fjern(finnOpplysning(opplysningId))
    }

    fun fjern(opplysning: Opplysning<*>) {
        // Fjern alle opplysninger som er utledet av opplysningen som fjernes
        fjernAvhengigheter(opplysning)

        egne.remove(opplysning)
        fjernet.add(opplysning)

        alleOpplysninger.refresh()
    }

    private fun fjernAvhengigheter(eksisterende: Opplysning<*>) {
        val graf = OpplysningGraf(aktiveOpplysningerListe)
        val avhengigheter = graf.hentAlleUtledetAv(eksisterende)
        avhengigheter.forEach { avhengighet -> fjern(avhengighet) }
    }

    private val opplysningerSomErErstattet get() = egne.mapNotNull { it.erstatter }.map { it.id }

    private fun Collection<Opplysning<*>>.utenErstattet(): List<Opplysning<*>> {
        val opplysningerSomErErstattet = opplysningerSomErErstattet
        return filterNot { it.id in opplysningerSomErErstattet }
    }
}

private val logger = KotlinLogging.logger {}

class OpplysningIkkeFunnetException(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)
