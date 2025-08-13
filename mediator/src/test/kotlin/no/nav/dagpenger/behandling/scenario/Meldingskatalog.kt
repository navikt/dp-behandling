package no.nav.dagpenger.behandling.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.regel.OpplysningsTyper.søknadId
import java.time.LocalDate
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

    fun opprettBehandling(
        ident: String,
        gjelder: LocalDate,
    ) = JsonMessage
        .newMessage(
            "opprett_behandling",
            mapOf(
                "ident" to ident,
                "prøvingsdato" to gjelder,
                "begrunnelse" to "Automatisk opprettet av test",
            ),
        ).toJson()
}
