package no.nav.dagpenger.opplysning

import no.nav.dagpenger.uuid.UUIDv7
import java.lang.Exception
import java.time.LocalDate
import java.util.UUID

class Opplysninger private constructor(
    override val id: UUID,
    opplysninger: List<Opplysning<*>> = emptyList(),
    basertPå: List<Opplysninger> = emptyList(),
) : LesbarOpplysninger {
    private val opplysninger: MutableList<Opplysning<*>> = opplysninger.toMutableList()
    private val basertPåOpplysninger: List<Opplysning<*>> = basertPå.flatMap { it.basertPåOpplysninger + it.opplysninger }.toList()

    // TODO: Vi må se på om denne kan få bedre ytelse
    private val alleOpplysninger: List<Opplysning<*>>
        get() = (basertPåOpplysninger + opplysninger).filterNot { it.erErstattet }.filterNot { it.erFjernet }

    constructor() : this(UUIDv7.ny(), emptyList(), emptyList())
    constructor(id: UUID, opplysninger: List<Opplysning<*>>) : this(id, opplysninger, emptyList())
    constructor(opplysninger: List<Opplysning<*>>, basertPå: List<Opplysninger> = emptyList()) : this(UUIDv7.ny(), opplysninger, basertPå)
    constructor(vararg basertPå: Opplysninger) : this(emptyList(), basertPå.toList())

    val aktiveOpplysninger get() = opplysninger.toList()

    override fun forDato(gjelderFor: LocalDate): LesbarOpplysninger {
        // TODO: Erstatt med noe collectorgreier får å unngå at opplysninger som er erstattet blir med
        val opplysningerForDato =
            opplysninger
                .filter { it.gyldighetsperiode.inneholder(gjelderFor) }
                .filterNot { it.erErstattet }
                .filterNot { it.erFjernet }
        return Opplysninger(UUIDv7.ny(), opplysningerForDato)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Comparable<T>> leggTil(opplysning: Opplysning<T>) {
        val eksisterende = finnNullableOpplysning(opplysning.opplysningstype)

        if (eksisterende == null) {
            opplysninger.add(opplysning)
            return
        }

        if (basertPåOpplysninger.contains(eksisterende)) {
            // Endre gyldighetsperiode på gammel opplysning og legg til ny opplysning kant i kant
            val erstattes: Opplysning<T>? = alleOpplysninger.find { it.overlapper(opplysning) } as Opplysning<T>?
            if (erstattes !== null) {
                when {
                    opplysning.overlapperHalenAv(erstattes) -> {
                        // Overlapp på halen av eksisterende opplysning
                        val forkortet = erstattes.lagErstatning(opplysning)
                        opplysninger.add(forkortet)
                        opplysninger.add(opplysning)
                    }

                    erstattes.harSammegyldighetsperiode(opplysning) -> {
                        // Overlapp for samme periode
                        opplysninger.addAll(erstattes.erstattesAv(opplysning))
                    }

                    opplysning.starterFørOgOverlapper(erstattes) -> {
                        // Overlapp på starten av eksisterende opplysning
                        erstattes.erstattesAv(opplysning)
                        opplysninger.add(opplysning)
                    }

                    else -> {
                        throw IllegalArgumentException("Kan ikke legge til opplysning som overlapper med eksisterende opplysning")
                    }
                }
                return
            }
        }

        if (opplysninger.contains(eksisterende)) {
            // Erstatt hele opplysningen
            eksisterende.fjern()
            opplysninger.add(opplysning)
            return
        }

        opplysninger.add(opplysning)
    }

    internal fun <T : Comparable<T>> leggTilUtledet(opplysning: Opplysning<T>) = leggTil(opplysning)

    override fun <T : Comparable<T>> finnOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T> =
        finnNullableOpplysning(opplysningstype) ?: throw IllegalStateException("Har ikke opplysning $opplysningstype som er gyldig")

    override fun finnOpplysning(opplysningId: UUID) =
        opplysninger.singleOrNull { it.id == opplysningId }
            ?: throw OpplysningIkkeFunnetException("Har ikke opplysning med id=$opplysningId")

    override fun har(opplysningstype: Opplysningstype<*>) = alleOpplysninger.any { it.er(opplysningstype) }

    override fun finnAlle(opplysningstyper: List<Opplysningstype<*>>) =
        opplysningstyper.flatMap { type -> alleOpplysninger.filter { it.er(type) } }

    override fun finnAlle() = alleOpplysninger.toList()

    fun fjernet(): List<Opplysning<*>> = opplysninger.filter { it.erFjernet }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Comparable<T>> finnNullableOpplysning(opplysningstype: Opplysningstype<T>): Opplysning<T>? {
        if (alleOpplysninger.count { it.er(opplysningstype) } > 1) {
            throw IllegalStateException(
                """Har mer enn 1 opplysning av type $opplysningstype i opplysningerId=$id.
                |Fant ${alleOpplysninger.count { it.er(opplysningstype) }} duplikater blant ${alleOpplysninger.size} opplysninger.
                |Basert på (${basertPåOpplysninger.size} sett) ${basertPåOpplysninger.joinToString { it.id.toString() }}
                """.trimMargin(),
            )
        }
        return alleOpplysninger.singleOrNull { it.er(opplysningstype) } as Opplysning<T>?
    }

    private fun <T : Comparable<T>> Opplysning<T>.overlapperHalenAv(opplysning: Opplysning<T>) =
        this.gyldighetsperiode.fom.isAfter(opplysning.gyldighetsperiode.fom) &&
            this.gyldighetsperiode.fom <= opplysning.gyldighetsperiode.tom

    private fun <T : Comparable<T>> Opplysning<T>.harSammegyldighetsperiode(opplysning: Opplysning<T>) =
        this.gyldighetsperiode == opplysning.gyldighetsperiode

    private fun <T : Comparable<T>> Opplysning<T>.starterFørOgOverlapper(opplysning: Opplysning<T>) =
        this.gyldighetsperiode.fom.isBefore(opplysning.gyldighetsperiode.fom) &&
            opplysning.gyldighetsperiode.inneholder(this.gyldighetsperiode.tom)

    operator fun plus(tidligereOpplysninger: List<Opplysninger>) = Opplysninger(id, opplysninger, tidligereOpplysninger)
}

class OpplysningIkkeFunnetException(
    message: String,
    exception: Exception? = null,
) : RuntimeException(message, exception)
