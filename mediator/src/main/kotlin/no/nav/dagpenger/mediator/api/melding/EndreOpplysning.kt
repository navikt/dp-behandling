package no.nav.dagpenger.mediator.api.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.util.UUID

internal data class EndreOpplysning(
    val behandlingId: UUID,
    val opplysningId: UUID,
    val behovId: String,
    val ident: String,
    val verdi: Any?,
    val gyldigFraOgMed: LocalDate?,
    val gyldigTilOgMed: LocalDate?,
    val saksbehandler: String,
    val begrunnelse: String,
) {
    fun toJson(): String =
        JsonMessage
            .newNeed(
                listOf("EndreOpplysning"),
                mapOf(
                    "@final" to true,
                    "behandlingId" to behandlingId,
                    "opplysningId" to opplysningId,
                    "behovId" to behovId,
                    "ident" to ident,
                    "saksbehandler" to saksbehandler,
                    "begrunnelse" to begrunnelse,
                ) +
                    listOfNotNull(
                        verdi?.let { "verdi" to it },
                        gyldigFraOgMed?.let { "gyldigFraOgMed" to it },
                        gyldigTilOgMed?.let { "gyldigTilOgMed" to it },
                    ),
            ).toJson()
            .also {
                sikkerlogg.info { "Endre opplysning fra APIet: $it" }
            }

    private companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.EndreOpplysning")
    }
}
