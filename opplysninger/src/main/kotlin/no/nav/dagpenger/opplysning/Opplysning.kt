package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.Regel
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Utledning(val regel: String, val opplysninger: List<Opplysning<*>>) {
    internal constructor(regel: Regel<*>, opplysninger: List<Opplysning<*>>) : this(regel::class.java.simpleName, opplysninger)
}

sealed class Opplysning<T : Comparable<T>>(
    val id: UUID,
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val gyldighetsperiode: Gyldighetsperiode,
    val utledetAv: Utledning?,
    val kilde: Kilde?,
    val opprettet: LocalDateTime,
    private var _erstatter: Opplysning<T>? = null,
    private val _erstattetAv: MutableList<Opplysning<T>> = mutableListOf(),
) : Klassifiserbart by opplysningstype {
    abstract fun bekreft(): Faktum<T>

    val erstatter get() = _erstatter

    val erErstattet get() = _erstattetAv.isNotEmpty()

    val erstattetAv get() = _erstattetAv.toList()

    val kanRedigeres get() = utledetAv == null && opplysningstype.datatype != ULID && !erErstattet

    fun overlapper(opplysning: Opplysning<*>) =
        opplysningstype == opplysning.opplysningstype && gyldighetsperiode.overlapp(opplysning.gyldighetsperiode)

    override fun equals(other: Any?) = other is Opplysning<*> && id == other.id

    override fun hashCode() = id.hashCode()

    override fun toString() = "${javaClass.simpleName} om ${opplysningstype.navn} har verdi: $verdi som er $gyldighetsperiode"

    fun erstattesAv(vararg erstatning: Opplysning<T>): List<Opplysning<T>> {
        val erstatninger = erstatning.toList().onEach { it._erstatter = this }
        _erstattetAv.addAll(erstatninger)
        return erstatninger
    }

    abstract fun lagErstatning(opplysning: Opplysning<T>): Opplysning<T>
}

class Hypotese<T : Comparable<T>>(
    id: UUID,
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
    utledetAv: Utledning? = null,
    kilde: Kilde? = null,
    opprettet: LocalDateTime,
    erstatter: Opplysning<T>? = null,
) : Opplysning<T>(id, opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet, erstatter) {
    constructor(
        opplysningstype: Opplysningstype<T>,
        verdi: T,
        gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
        utledetAv: Utledning? = null,
        kilde: Kilde? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
        erstatter: Opplysning<T>? = null,
    ) : this(UUIDv7.ny(), opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet, erstatter)

    override fun bekreft() = Faktum(id, super.opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet)

    override fun lagErstatning(opplysning: Opplysning<T>) =
        Hypotese(
            opplysningstype,
            verdi,
            gyldighetsperiode.kopi(tom = opplysning.gyldighetsperiode.fom.minusDays(1)),
            utledetAv,
            kilde,
            opplysning.opprettet,
            erstatter = this,
        )
}

class Faktum<T : Comparable<T>>(
    id: UUID,
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
    utledetAv: Utledning? = null,
    kilde: Kilde? = null,
    opprettet: LocalDateTime,
    erstatter: Opplysning<T>? = null,
) : Opplysning<T>(id, opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet, erstatter) {
    constructor(
        opplysningstype: Opplysningstype<T>,
        verdi: T,
        gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
        utledetAv: Utledning? = null,
        kilde: Kilde? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
        erstatter: Opplysning<T>? = null,
    ) : this(UUIDv7.ny(), opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet, erstatter)

    override fun bekreft() = this

    override fun lagErstatning(opplysning: Opplysning<T>) =
        Faktum(
            opplysningstype,
            verdi,
            gyldighetsperiode.kopi(tom = opplysning.gyldighetsperiode.fom.takeIf { it != LocalDate.MIN }?.minusDays(1) ?: LocalDate.MIN),
            utledetAv,
            kilde,
            opplysning.opprettet,
            this,
        )
}
