package no.nav.dagpenger.avklaring

import no.nav.dagpenger.avklaring.Avklaring.Endring.Avbrutt
import no.nav.dagpenger.avklaring.Avklaring.Endring.Avklart
import no.nav.dagpenger.avklaring.Avklaring.Endring.UnderBehandling
import no.nav.dagpenger.opplysning.Avklaringkode
import no.nav.dagpenger.opplysning.Kilde
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.opplysning.Saksbehandlerkilde
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDateTime
import java.util.UUID

data class Avklaring(
    val id: UUID,
    val kode: Avklaringkode,
    private val historikk: MutableList<Endring> = mutableListOf(UnderBehandling()),
) {
    constructor(
        kode: Avklaringkode,
        grunnlag: Set<UUID> = emptySet(),
    ) : this(UUIDv7.ny(), kode, mutableListOf(UnderBehandling(grunnlag = grunnlag)))

    private val tilstand get() = endringer.last()
    val sistEndret get(): LocalDateTime = endringer.last().endret

    val endringer get() = historikk.sorted().toList()

    val kanKvitteres = kode.kanKvitteres

    private val sistAktiveGrunnlag: Set<UUID>
        get() = historikk.filterIsInstance<UnderBehandling>().maxByOrNull { it.endret }?.grunnlag ?: emptySet()

    // Returnerer true om minst én av opplysningene som trigget siste åpning er erstattet.
    fun erGrunnlagetEndret(opplysninger: LesbarOpplysninger): Boolean = sistAktiveGrunnlag.any { opplysninger.erErstattet(it) }

    // Antall endringer ved rehydrering — brukes for dirty-tracking
    private var lagretEndringerCount = 0

    val nyeEndringer get() = historikk.sortedBy { it.endret }.drop(lagretEndringerCount)

    val erNy get() = lagretEndringerCount == 0

    fun markerLagret() {
        lagretEndringerCount = historikk.size
    }

    fun måAvklares() = tilstand is UnderBehandling

    fun erAvklart() = tilstand is Avklart

    fun erAvbrutt() = tilstand is Avbrutt

    internal fun avbryt(): Boolean = historikk.add(Avbrutt())

    internal fun kvitter(
        saksbehandlerkilde: Saksbehandlerkilde,
        begrunnelse: String,
    ): Boolean {
        kanKvitteresSjekk()
        return historikk.add(Avklart(avklartAv = saksbehandlerkilde, begrunnelse = begrunnelse))
    }

    internal fun avklar(kilde: Kilde): Boolean = historikk.add(Avklart(avklartAv = kilde))

    internal fun gjenåpne(grunnlag: Set<UUID> = emptySet()): Boolean = historikk.add(UnderBehandling(grunnlag = grunnlag))

    private fun kanKvitteresSjekk() {
        require(kanKvitteres) { "Avklaring $kode kan ikke kvitteres ut, krever endring i behandlingen" }
    }

    fun løstAvSaksbehandler() = tilstand is Avklart && (tilstand as Avklart).avklartAv is Saksbehandlerkilde

    sealed class Endring(
        val id: UUID,
        open val endret: LocalDateTime,
    ) : Comparable<Endring> {
        override fun compareTo(other: Endring) = endret.compareTo(other.endret)

        class UnderBehandling(
            id: UUID = UUIDv7.ny(),
            endret: LocalDateTime = LocalDateTime.now(),
            val grunnlag: Set<UUID> = emptySet(),
        ) : Endring(id, endret)

        class Avklart(
            id: UUID = UUIDv7.ny(),
            val avklartAv: Kilde,
            val begrunnelse: String = "",
            endret: LocalDateTime = LocalDateTime.now(),
        ) : Endring(id, endret)

        class Avbrutt(
            id: UUID = UUIDv7.ny(),
            endret: LocalDateTime = LocalDateTime.now(),
        ) : Endring(id, endret)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Avklaring) return false
        return kode == other.kode
    }

    override fun hashCode() = kode.hashCode()

    companion object {
        fun rehydrer(
            id: UUID,
            kode: Avklaringkode,
            historikk: MutableList<Endring>,
        ) = Avklaring(id, kode, historikk).also {
            it.lagretEndringerCount = historikk.size
        }
    }
}
