package no.nav.dagpenger.modell

import java.time.LocalDate
import java.util.UUID

data class Utestengningsperiode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val behandlingId: UUID,
    val behandlingskjedeId: UUID,
)
