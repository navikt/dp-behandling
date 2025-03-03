package no.nav.dagpenger.behandling.modell.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration

open class MeldekortHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    val meldekortId: Long,
    val fom: LocalDate,
    val tom: LocalDate,
    val kilde: MeldekortKilde,
    val dager: List<Dag>,
    opprettet: LocalDateTime,
) : PersonHendelse(meldingsreferanseId, ident, opprettet)

data class MeldekortKilde(
    val rolle: String,
    val ident: String,
)

data class Dag(
    val dato: LocalDate,
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
    Fravaer,
}
