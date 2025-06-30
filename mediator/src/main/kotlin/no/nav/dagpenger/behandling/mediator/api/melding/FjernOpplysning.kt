package no.nav.dagpenger.behandling.mediator.api.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import mu.KotlinLogging
import java.util.UUID

internal data class FjernOpplysning(
    val behandlingId: UUID,
    val opplysningId: UUID,
    val behovId: String,
    val ident: String,
    val saksbehandler: String,
) {
    fun toJson(): String =
        JsonMessage
            .newNeed(
                listOf("FjernOpplysning"),
                mapOf(
                    "@final" to true,
                    "behandlingId" to behandlingId,
                    "opplysningId" to opplysningId,
                    "behovId" to behovId,
                    "ident" to ident,
                ),
            ).toJson()
            .also {
                sikkerlogg.info { "Fjerne opplysning fra APIet: $it" }
            }

    private companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.FjernOpplysning")
    }
}
