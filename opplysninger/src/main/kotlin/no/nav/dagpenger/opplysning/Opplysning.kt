package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Utledning(
    val regel: String,
    val opplysninger: List<Opplysning<*>>,
) {
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
    private var _erRelevant: Boolean = true,
    private var _erstatter: Opplysning<T>? = null,
    private var _skalLagres: Boolean = false,
) : Klassifiserbart by opplysningstype {
    private val defaultRedigering = Redigerbar { opplysningstype.datatype != ULID }

    abstract fun bekreft(): Faktum<T>

    val skalLagres get() = _skalLagres

    val erstatter get() = _erstatter

    val erRelevant get() = _erRelevant

    fun erRelevant(relevant: Boolean) {
        _erRelevant = relevant
        _skalLagres = true
    }

    val kanRedigeres: (Redigerbar) -> Boolean
        get() = { redigerbar ->
            redigerbar.kanRedigere(opplysningstype) &&
                defaultRedigering.kanRedigere(opplysningstype)
        }

    fun overlapper(opplysning: Opplysning<*>) =
        opplysningstype.er(opplysning.opplysningstype) && gyldighetsperiode.overlapp(opplysning.gyldighetsperiode)

    override fun equals(other: Any?) = other is Opplysning<*> && id == other.id

    override fun hashCode() = id.hashCode()

    override fun toString() = "${javaClass.simpleName} om ${opplysningstype.navn} har verdi: $verdi som er $gyldighetsperiode"

    fun erstatter(erstattet: Opplysning<T>) {
        _erstatter = erstattet
    }

    fun overlapperHalenAv(opplysning: Opplysning<T>) =
        gyldighetsperiode.fom.isAfter(opplysning.gyldighetsperiode.fom) &&
            gyldighetsperiode.fom <= opplysning.gyldighetsperiode.tom

    fun harSammegyldighetsperiode(opplysning: Opplysning<T>) = gyldighetsperiode == opplysning.gyldighetsperiode

    fun starterFørOgOverlapper(opplysning: Opplysning<T>) =
        this.gyldighetsperiode.fom.isBefore(opplysning.gyldighetsperiode.fom) &&
            opplysning.gyldighetsperiode.inneholder(gyldighetsperiode.tom)

    abstract fun lagForkortet(opplysning: Opplysning<T>): Opplysning<T>

    abstract fun nyID(): Opplysning<T>

    companion object {
        fun Collection<Opplysning<*>>.gyldigeFor(dato: LocalDate) = filter { it.gyldighetsperiode.inneholder(dato) }
    }
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
    skalLagres: Boolean = true,
) : Opplysning<T>(id, opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet, true, erstatter, _skalLagres = skalLagres) {
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

    override fun nyID(): Opplysning<T> {
        TODO("Not yet implemented")
    }

    override fun lagForkortet(opplysning: Opplysning<T>) =
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
    skalLagres: Boolean = true,
) : Opplysning<T>(id, opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet, true, erstatter, _skalLagres = skalLagres) {
    constructor(
        opplysningstype: Opplysningstype<T>,
        verdi: T,
        gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
        utledetAv: Utledning? = null,
        kilde: Kilde? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
        erstatter: Opplysning<T>? = null,
    ) : this(UUIDv7.ny(), opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet, erstatter)

    init {
        opplysningstype.enhet?.valider(verdi)
    }

    override fun bekreft() = this

    /* Metode for å lage en ny instans av Faktum med en ny UUID. Vi sorterer etter ID,
     * og når vi forkorter tidligere opplysning må vi få ny UUID som kommer senere enn den forkortede opplysningen.
     */
    override fun nyID() =
        Faktum(
            UUIDv7.ny(),
            opplysningstype,
            verdi,
            gyldighetsperiode,
            utledetAv,
            kilde,
            opprettet,
            erstatter,
            skalLagres,
        )

    override fun lagForkortet(opplysning: Opplysning<T>) =
        Faktum(
            opplysningstype,
            verdi,
            gyldighetsperiode.kopi(
                tom =
                    opplysning.gyldighetsperiode.fom
                        .takeIf { !it.isEqual(LocalDate.MIN) }
                        ?.minusDays(1) ?: LocalDate.MIN,
            ),
            utledetAv,
            kilde,
            opplysning.opprettet,
            this,
        )

    fun somEnhet() = opplysningstype.enhet?.somEnhet(verdi)
}
