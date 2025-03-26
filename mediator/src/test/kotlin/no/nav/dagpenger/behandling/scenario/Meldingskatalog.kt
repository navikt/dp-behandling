package no.nav.dagpenger.behandling.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.UUID

internal object Meldingskatalog {
    fun søknadInnsendt(
        ident: String,
        innsendt: LocalDateTime,
        fagsakId: Int,
        søknadId: UUID,
    ) = JsonMessage
        .newMessage(
            "søknad_behandlingsklar",
            mapOf(
                "innsendt" to innsendt,
                "ident" to ident,
                "fagsakId" to fagsakId,
                "bruk-dp-behandling" to true,
                "søknadId" to søknadId,
            ),
        ).toJson()
}
