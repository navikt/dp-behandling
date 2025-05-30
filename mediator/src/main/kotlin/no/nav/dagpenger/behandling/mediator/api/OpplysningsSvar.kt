package no.nav.dagpenger.behandling.mediator.api

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.LocalDate
import java.util.UUID

internal data class OpplysningsSvar(
    val behandlingId: UUID,
    val opplysningId: UUID,
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

        return JsonMessage
            .newNeed(
                listOf(opplysningNavn),
                mapOf(
                    "@final" to true,
                    "@opplysningsbehov" to true,
                    "opplysningId" to opplysningId,
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
    }
}
