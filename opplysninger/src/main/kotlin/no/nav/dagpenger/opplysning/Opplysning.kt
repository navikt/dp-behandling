package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.regel.Regel
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Utledning(
    val regel: String,
    val opplysninger: List<Opplysning<*>>,
    val versjon: String? = System.getenv("NAIS_APP_IMAGE"),
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
    private var _erstatter: Opplysning<T>? = null,
    private var _skalLagres: Boolean = false,
) : Klassifiserbart by opplysningstype {
    private val defaultRedigering = Redigerbar { opplysningstype.datatype != ULID }

    abstract fun bekreft(): Faktum<T>

    val skalLagres get() = _skalLagres

    val erstatter get() = _erstatter

    val kanRedigeres: (Redigerbar) -> Boolean
        get() = { redigerbar ->
            redigerbar.kanRedigere(opplysningstype) &&
                defaultRedigering.kanRedigere(opplysningstype)
        }

    override fun equals(other: Any?) = other is Opplysning<*> && id == other.id

    override fun hashCode() = id.hashCode()

    override fun toString() = "${javaClass.simpleName} om ${opplysningstype.navn} har verdi: $verdi som er $gyldighetsperiode"

    fun erstatter(erstattet: Opplysning<T>) {
        _erstatter = erstattet
    }

    companion object {
        fun Collection<Opplysning<*>>.gyldigeFor(dato: LocalDate) = filter { it.gyldighetsperiode.inneholder(dato) }
    }

    abstract fun lagForkortet(tilOgMed: Opplysning<*>): Opplysning<T>
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
) : Opplysning<T>(id, opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet, erstatter, _skalLagres = skalLagres) {
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

    override fun lagForkortet(tilOgMed: Opplysning<*>): Opplysning<T> {
        TODO("Not yet implemented")
    }
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
) : Opplysning<T>(id, opplysningstype, verdi, gyldighetsperiode, utledetAv, kilde, opprettet, erstatter, _skalLagres = skalLagres) {
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

    fun somEnhet() = opplysningstype.enhet?.somEnhet(verdi)

    override fun lagForkortet(tilOgMed: Opplysning<*>): Opplysning<T> {
        val forrigeFom =
            tilOgMed.gyldighetsperiode.fraOgMed
                .takeUnless { it.isEqual(LocalDate.MIN) }
                ?.minusDays(1) ?: LocalDate.MIN
        /*require(gyldighetsperiode.fraOgMed.isBefore(forrigeFom)) {
            """Kan ikke forkorte gyldighetsperiode fra ${gyldighetsperiode.fraOgMed} til $forrigeFom.
                |Gjelder opplysningstype: ${opplysningstype.navn}, id: $id
            """.trimMargin()
        }*/
        return Faktum(
            id,
            opplysningstype,
            verdi,
            Gyldighetsperiode(fraOgMed = gyldighetsperiode.fraOgMed, tilOgMed = forrigeFom),
            utledetAv,
            kilde,
            opprettet,
            erstatter,
        )
    }
}
