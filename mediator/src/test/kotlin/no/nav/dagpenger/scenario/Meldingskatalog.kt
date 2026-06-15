package no.nav.dagpenger.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.opplysning.verdier.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal sealed class MeldekortAktivitet {
    data class Arbeid(
        val timer: Int,
    ) : MeldekortAktivitet()

    data object Syk : MeldekortAktivitet()

    data object Fravær : MeldekortAktivitet()

    data class Utdanning(
        val timer: Int,
    ) : MeldekortAktivitet()

    data object IngenAktivitet : MeldekortAktivitet()
}

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

    fun fåAnnenYtelse(
        ident: String,
        fraOgMed: LocalDateTime = LocalDateTime.now(),
        tema: String = "SYK",
    ) = JsonMessage
        .newMessage(
            "annen_ytelse_endret",
            mapOf(
                "ident" to ident,
                "tidspunkt" to fraOgMed,
                "tema" to tema,
            ),
        ).toJson()

    @JvmName("meldekortInnsendtMedArbeidstimer")
    fun meldekortInnsendt(
        ident: String,
        meldekortId: UUID,
        meldeperiode: Periode,
        korrigeringAv: UUID? = null,
        arbeidstimer: List<Int> = emptyList(),
    ): String =
        meldekortInnsendt(
            ident = ident,
            meldekortId = meldekortId,
            meldeperiode = meldeperiode,
            korrigeringAv = korrigeringAv,
            aktiviteter = arbeidstimer.map { MeldekortAktivitet.Arbeid(it) },
        )

    fun meldekortInnsendt(
        ident: String,
        meldekortId: UUID,
        meldeperiode: Periode,
        korrigeringAv: UUID? = null,
        aktiviteter: List<MeldekortAktivitet> = emptyList(),
        meldedato: LocalDate = meldeperiode.tilOgMed.plusDays(1),
    ): String =
        JsonMessage
            .newMessage(
                "meldekort_innsendt",
                buildMap {
                    putAll(
                        listOf(
                            "id" to meldekortId,
                            "ident" to ident,
                            "periode" to
                                mapOf(
                                    "fraOgMed" to meldeperiode.fraOgMed,
                                    "tilOgMed" to meldeperiode.tilOgMed,
                                ),
                            "dager" to
                                meldeperiode.mapIndexed { index, meldedag ->
                                    val aktivitet = aktiviteter.getOrElse(index) { MeldekortAktivitet.IngenAktivitet }
                                    mapOf(
                                        "dato" to meldedag,
                                        "meldt" to true,
                                        "aktiviteter" to
                                            when (aktivitet) {
                                                is MeldekortAktivitet.Arbeid -> {
                                                    listOf(mapOf("type" to "Arbeid", "timer" to "PT${aktivitet.timer}H"))
                                                }

                                                is MeldekortAktivitet.Syk -> {
                                                    listOf(mapOf("type" to "Syk"))
                                                }

                                                is MeldekortAktivitet.Fravær -> {
                                                    listOf(mapOf("type" to "Fravaer"))
                                                }

                                                is MeldekortAktivitet.Utdanning -> {
                                                    listOf(mapOf("type" to "Utdanning", "timer" to "PT${aktivitet.timer}H"))
                                                }

                                                is MeldekortAktivitet.IngenAktivitet -> {
                                                    emptyList<Map<String, Any>>()
                                                }
                                            },
                                    )
                                },
                            "kilde" to
                                mapOf(
                                    "rolle" to "Søker",
                                    "ident" to ident,
                                ),
                            "meldedato" to meldedato,
                            "innsendtTidspunkt" to LocalDateTime.now(),
                        ),
                    )
                    if (korrigeringAv != null) {
                        put("originalMeldekortId", "$korrigeringAv")
                    }
                },
            ).toJson()

    fun omgjørBehandling(
        ident: String,
        gjelderDato: LocalDate,
    ) = JsonMessage
        .newMessage(
            "omgjør_behandling",
            mapOf(
                "ident" to ident,
                "gjelderDato" to gjelderDato,
            ),
        ).toJson()

    fun avsluttArbeidssøkerperiode(
        ident: String,
        fastsattMeldedato: LocalDate = LocalDate.now(),
        avregistrertTidspunkt: LocalDateTime = LocalDateTime.now(),
        fristBrutt: Boolean = false,
        manueltAvregistrert: Boolean = false,
    ) = JsonMessage
        .newMessage(
            "avsluttet_arbeidssokerperiode",
            buildMap {
                val periodeId = UUID.randomUUID()
                put("periodeId", periodeId)
                put("ident", ident)
                put("fastsattMeldedato", fastsattMeldedato)
                put("avregistrertTidspunkt", avregistrertTidspunkt)
                when {
                    fristBrutt -> put("årsak", "IKKE_MELDT_SEG_PÅ_21_DAGER")
                    manueltAvregistrert -> put("årsak", "UTMELDT_I_ARBEIDSSØKERREGISTERET")
                    else -> put("årsak", "UTMELDT_PÅ_MELDEKORT")
                }
            },
        ).toJson()
}
