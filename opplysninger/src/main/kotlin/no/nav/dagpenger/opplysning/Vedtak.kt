package no.nav.dagpenger.opplysning

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface Rettighet {
    val løpende: Boolean
}

interface Regelverkstype

interface Vedtak<F : Regelverkstype> {
    val vedtakId: UUID
    val vedtaksdato: LocalDate
    val virkningsdato: LocalDate
    val vilkår: List<Vilkår>
    val fastsatt: F
    val utbetalinger: List<Utbetaling>
    val utfall: Boolean

    fun blurp(block: (Rettighet) -> Unit)
}

data class Vilkår(
    val navn: String,
    val hjemmel: String,
    val vurderingstidspunkt: LocalDateTime,
    val status: Boolean,
)

data class Utbetaling(
    val dato: LocalDate,
    val beløp: Int,
)

data class NoeGøyFraBehandlingen(
    val behandlingId: UUID,
)
