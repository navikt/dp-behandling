package no.nav.dagpenger.behandling.mediator.api.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import mu.KotlinLogging
import java.time.LocalDate
import java.util.UUID

internal data class OpplysningsSvar(
    val behandlingId: UUID,
    val opplysningNavn: String,
    val ident: String,
    val verdi: Any,
    val saksbehandler: String,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
) {
    fun toJson(): String {
        val gyldighetsperiode =
            listOfNotNull(
                gyldigFraOgMed?.let { "gyldigFraOgMed" to it },
                gyldigTilOgMed?.let { "gyldigTilOgMed" to it },
            ).toMap()

        return JsonMessage.Companion
            .newNeed(
                listOf(opplysningNavn),
                mapOf(
                    "@final" to true,
                    "@opplysningsbehov" to true,
                    "behandlingId" to behandlingId,
                    "ident" to ident,
                    "@løsning" to
                        mapOf(
                            opplysningNavn to
                                mapOf(
                                    "verdi" to verdi,
                                    "@kilde" to
                                        mapOf(
                                            "saksbehandler" to saksbehandler,
                                        ),
                                ) + gyldighetsperiode,
                        ),
                ),
            ).toJson()
            .also {
                sikkerlogg.info { "Publiserer opplysningsvar fra APIet: $it" }
            }
    }

    private companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.OpplysningsSvar")
    }
}
