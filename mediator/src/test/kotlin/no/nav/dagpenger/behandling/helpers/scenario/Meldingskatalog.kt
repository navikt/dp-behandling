package no.nav.dagpenger.behandling.helpers.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.opplysning.verdier.Periode
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

    fun meldekortInnsendt(
        ident: String,
        meldekortId: UUID,
        meldeperiode: Periode,
        korrigeringAv: UUID? = null,
        arbeidstimer: List<Int> = emptyList(),
    ): String =
        JsonMessage
            .newMessage(
                "meldekort_innsendt",
                mapOf(
                    "id" to meldekortId,
                    "ident" to ident,
                    "periode" to
                        mapOf(
                            "fraOgMed" to meldeperiode.fraOgMed,
                            "tilOgMed" to meldeperiode.tilOgMed,
                        ),
                    "korrigeringAv" to "$korrigeringAv",
                    "dager" to
                        meldeperiode.mapIndexed { index, meldedag ->
                            val timer = arbeidstimer.getOrElse(index) { 0 }
                            mapOf(
                                "dato" to meldedag,
                                "meldt" to true,
                                "aktiviteter" to
                                    listOf(
                                        mapOf("type" to "Arbeid", "timer" to "PT${timer}H"),
                                    ),
                            )
                        },
                    "kilde" to
                        mapOf(
                            "rolle" to "Søker",
                            "ident" to ident,
                        ),
                    "innsendtTidspunkt" to LocalDateTime.now(),
                ),
            ).toJson()

    fun beregnMeldekort(
        ident: String,
        meldekortId: UUID,
    ) = JsonMessage
        .newMessage(
            "beregn_meldekort",
            mapOf(
                "meldekortId" to meldekortId,
                "ident" to ident,
            ),
        ).toJson()
}
