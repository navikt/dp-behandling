package no.nav.dagpenger.behandling.modell.hendelser

import no.nav.dagpenger.opplysning.verdier.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration

class MeldekortInnsendtHendelse(
    meldingsreferanseId: UUID,
    val meldekort: Meldekort,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, meldekort.ident, opprettet)

data class Meldekort(
    val id: UUID,
    val meldingsreferanseId: UUID,
    val ident: String,
    val eksternMeldekortId: Long,
    val fom: LocalDate,
    val tom: LocalDate,
    val kilde: MeldekortKilde,
    val dager: List<Dag>,
    val innsendtTidspunkt: LocalDateTime,
    val korrigeringAv: Long?,
) : Comparable<Meldekort> {
    fun periode() = Periode(fom, tom)

    override fun compareTo(other: Meldekort) = fom.compareTo(other.fom)
}

data class MeldekortKilde(
    val rolle: String,
    val ident: String,
)

data class Dag(
    val dato: LocalDate,
    val meldt: Boolean,
    val aktiviteter: List<MeldekortAktivitet>,
)

data class MeldekortAktivitet(
    val type: AktivitetType,
    val timer: Duration?,
)

enum class AktivitetType {
    Arbeid,
    Syk,
    Utdanning,
    Fravær,
}
