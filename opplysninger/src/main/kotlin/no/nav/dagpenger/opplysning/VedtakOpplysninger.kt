package no.nav.dagpenger.opplysning

import no.nav.dagpenger.opplysning.verdier.Beløp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class VedtakOpplysninger(
    val vedtakId: UUID,
    val vedtaksdato: LocalDate,
    val virkningsdato: LocalDate,
    val vilkår: List<Vilkår>,
    val fastsatt: Fastsatt,
    val utbetalinger: List<Utbetaling>,
    val behandletAv: List<BehandletAv>,
    val behandletHendelse: Hendelse,
) {
    val utfall = vilkår.all { it.status }
}

data class Hendelse(
    val datatype: String,
    val id: String,
    val type: Type,
) {
    /**
     *
     * Values: Søknad,Meldekort
     */
    enum class Type(
        val value: String,
    ) {
        Søknad("Søknad"),
        Meldekort("Meldekort"),
    }
}

data class BehandletAv(
    val rolle: BehandletAv.Rolle,
    val behandler: Saksbehandler? = null,
) {
    @Suppress("ktlint:standard:enum-entry-name-case")
    enum class Rolle(
        val value: String,
    ) {
        saksbehandler("saksbehandler"),
        beslutter("beslutter"),
    }
}

data class SaksbehandlerDTO(
    val ident: String,
)

data class Vilkår(
    val navn: String,
    val hjemmel: Hjemmel,
    val vurderingstidspunkt: LocalDateTime,
    val status: Boolean,
)

sealed class Fastsatt(
    val utfall: Boolean,
    val fastsattVanligArbeidstid: FastsattVanligArbeidstid?,
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

    class FastsattBuilder {
        private var utfall: Boolean = false
        private var grunnlag: Opplysningstype<Beløp>? = null
        private var fastsattVanligArbeidstid: Fastsatt.FastsattVanligArbeidstid? = null
        private var samordning: List<Samordning> = emptyList()

        fun utfall(utfall: Boolean) = apply { this.utfall = utfall }

        fun fastsattVanligArbeidstid(fastsattVanligArbeidstid: Fastsatt.FastsattVanligArbeidstid?) =
            apply { this.fastsattVanligArbeidstid = fastsattVanligArbeidstid }

        fun grunnlag(grunnlag: Opplysningstype<Beløp>) = apply { this.grunnlag = grunnlag }

        fun samordning(samordning: List<Samordning>) = apply { this.samordning = samordning }

        fun build(opplysninger: LesbarOpplysninger): Fastsatt =
            if (utfall) {
                Innvilgelse(
                    grunnlag =
                        grunnlag?.let {
                            opplysninger
                                .finnOpplysning(it)
                                .verdi.avrundet
                                .toInt()
                        }
                            ?: throw IllegalArgumentException("Forventet grunnlag"),
                    fastsattVanligArbeidstid = fastsattVanligArbeidstid ?: Fastsatt.FastsattVanligArbeidstid(0.0, 0.0),
                    sats = Fastsatt.Sats(0, emptyList()),
                    samordning = samordning,
                    kvoter = emptyList(),
                )
            } else {
                Avslag(
                    fastsattVanligArbeidstid = fastsattVanligArbeidstid ?: Fastsatt.FastsattVanligArbeidstid(0.0, 0.0),
                    samordning = samordning,
                )
            }
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
