package no.nav.dagpenger.behandling.modell.hendelser

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// Skulle vi ha arvet fra MeldekortHendelse her?
class MeldekortKorrigeringHendelse(
    meldingsreferanseId: UUID,
    ident: String,
    meldekortId: Long,
    fom: LocalDate,
    tom: LocalDate,
    kilde: MeldekortKilde,
    dager: List<Dag>,
    val orginalMeldekortId: Long,
    opprettet: LocalDateTime,
) : MeldekortHendelse(meldingsreferanseId, ident, meldekortId, fom, tom, kilde, dager, opprettet)
