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

sealed class Opplysning<T : Any>(
    val id: UUID,
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val gyldighetsperiode: Gyldighetsperiode,
    val utledetAv: Utledning?,
    val kilde: Kilde?,
    val opprettet: LocalDateTime,
    var erUtdatert: Boolean = false,
    private var _erstatter: Opplysning<T>? = null,
    private var _skalLagres: Boolean = false,
    // Flagg som indikerer om opplysningen har blitt behandlet av regelkjøringen.
    // Default true fordi opplysninger lastet fra DB allerede er behandlet (flagget persisteres ikke).
    var behandlet: Boolean = true,
    // Hvilken prøvingsdato opplysningen sist ble behandlet ved. Persisteres til DB.
    // Brukes for å skille opplysninger fra nåværende eval-syklus (kan fjernes ved cleanup)
    // fra opplysninger fra tidligere eval-sykluser med annen prøvingsdato (skal beskyttes).
    var behandletVed: LocalDate? = null,
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

    val skalArves get() = opplysningstype.opplysningstypeKategori == OpplysningstypeKategori.Materiell

    override fun equals(other: Any?) = other is Opplysning<*> && id == other.id

    override fun hashCode() = id.hashCode()

    override fun toString() = "${javaClass.simpleName} om ${opplysningstype.navn} har verdi: $verdi som er $gyldighetsperiode"

    fun erstatter(erstattet: Opplysning<T>) {
        _erstatter = erstattet
    }

    companion object {
        fun Collection<Opplysning<*>>.gyldigeFor(dato: LocalDate) = filter { it.gyldighetsperiode.inneholder(dato) }
    }

    abstract fun medGyldighetsperiode(gyldighetsperiode: Gyldighetsperiode): Opplysning<T>
}

class Hypotese<T : Any>(
    id: UUID,
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
    utledetAv: Utledning? = null,
    kilde: Kilde? = null,
    opprettet: LocalDateTime,
    erstatter: Opplysning<T>? = null,
    skalLagres: Boolean = true,
) : Opplysning<T>(
        id,
        opplysningstype,
        verdi,
        gyldighetsperiode,
        utledetAv,
        kilde,
        opprettet,
        false,
        erstatter,
        _skalLagres = skalLagres,
    ) {
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

    override fun medGyldighetsperiode(gyldighetsperiode: Gyldighetsperiode): Opplysning<T> =
        Hypotese(
            id,
            opplysningstype,
            verdi,
            gyldighetsperiode,
            utledetAv,
            kilde,
            opprettet,
            erstatter,
        )
}

class Faktum<T : Any>(
    id: UUID,
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
    utledetAv: Utledning? = null,
    kilde: Kilde? = null,
    opprettet: LocalDateTime,
    erstatter: Opplysning<T>? = null,
    skalLagres: Boolean = true,
) : Opplysning<T>(
        id,
        opplysningstype,
        verdi,
        gyldighetsperiode,
        utledetAv,
        kilde,
        opprettet,
        false,
        erstatter,
        _skalLagres = skalLagres,
    ) {
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

    override fun medGyldighetsperiode(gyldighetsperiode: Gyldighetsperiode) =
        Faktum(
            id,
            opplysningstype,
            verdi,
            gyldighetsperiode,
            utledetAv,
            kilde,
            opprettet,
            erstatter,
        )
}
