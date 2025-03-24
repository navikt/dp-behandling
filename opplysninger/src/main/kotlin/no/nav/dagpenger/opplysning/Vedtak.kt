package no.nav.dagpenger.opplysning

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Vedtak(
    val vedtakId: UUID,
    val vedtaksdato: LocalDate,
    val virkningsdato: LocalDate,
    val vilkår: List<Vilkår>,
    val fastsatt: Fastsatt,
    val utbetalinger: List<Utbetaling>,
) {
    val utfall = vilkår.all { it.status }
}

data class Vilkår(
    val navn: String,
    val hjemmel: String,
    val vurderingstidspunkt: LocalDateTime,
    val status: Boolean,
)

sealed class Fastsatt(
    val utfall: Boolean,
    val fastsattVanligArbeidstid: FastsattVanligArbeidstid,
    val samordning: List<Samordning>,
) {
    data class FastsattVanligArbeidstid(
        val fastsattVanligArbeidstidPerUke: Double,
        val nyArbeidstidPerUke: Double,
    )

    data class Sats(
        val dagsatsMedBarnetillegg: Int,
        val barn: List<Barn>,
    ) {
        data class Barn(
            val fødseldato: LocalDate,
            val girTillegg: Boolean,
        )
    }
}

class Innvilgelse(
    val grunnlag: Int,
    fastsattVanligArbeidstid: FastsattVanligArbeidstid,
    val sats: Sats,
    samordning: List<Samordning>,
    val kvoter: List<Kvote>,
) : Fastsatt(true, fastsattVanligArbeidstid, samordning)

class Avslag(
    fastsattVanligArbeidstid: FastsattVanligArbeidstid,
    samordning: List<Samordning>,
) : Fastsatt(false, fastsattVanligArbeidstid, samordning)

data class Samordning(
    val ytelse: String,
    val beløp: Int,
)

data class Kvote(
    val navn: String,
    val type: String,
    val verdi: Int,
)

data class Utbetaling(
    val dato: LocalDate,
    val beløp: Int,
)
