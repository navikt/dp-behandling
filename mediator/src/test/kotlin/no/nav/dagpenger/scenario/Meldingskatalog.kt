package no.nav.dagpenger.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.opplysning.verdier.Periode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.roundToInt

internal sealed class MeldekortAktivitet {
    data class Arbeid(
        val timer: Double,
    ) : MeldekortAktivitet() {
        constructor(timer: Int) : this(timer.toDouble())
    }

    data object Syk : MeldekortAktivitet()

    data object Fravær : MeldekortAktivitet()

    data class Utdanning(
        val timer: Double,
    ) : MeldekortAktivitet() {
        constructor(timer: Int) : this(timer.toDouble())
    }

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

    @JvmName("meldekortInnsendtMedArbeidstimerSomDouble")
    fun meldekortInnsendt(
        ident: String,
        meldekortId: UUID,
        meldeperiode: Periode,
        korrigeringAv: UUID? = null,
        korrigertAv: MeldekortKilde,
        arbeidstimer: List<Double>,
        meldtITide: Boolean = true,
    ): String =
        meldekortInnsendt(
            ident = ident,
            meldekortId = meldekortId,
            meldeperiode = meldeperiode,
            korrigeringAv = korrigeringAv,
            korrigertAv = korrigertAv,
            aktiviteter = arbeidstimer.map { MeldekortAktivitet.Arbeid(it) },
            meldtITide = meldtITide,
        )

    @JvmName("meldekortInnsendtMedArbeidstimer")
    fun meldekortInnsendt(
        ident: String,
        meldekortId: UUID,
        meldeperiode: Periode,
        korrigeringAv: UUID? = null,
        korrigertAv: MeldekortKilde,
        arbeidstimer: List<Int> = emptyList(),
        meldtITide: Boolean = true,
    ): String =
        meldekortInnsendt(
            ident = ident,
            meldekortId = meldekortId,
            meldeperiode = meldeperiode,
            korrigeringAv = korrigeringAv,
            korrigertAv = korrigertAv,
            aktiviteter = arbeidstimer.map { MeldekortAktivitet.Arbeid(it) },
            meldtITide = meldtITide,
        )

    data class MeldekortKilde(
        val kilde: Rolle,
        val ident: String,
    ) {
        enum class Rolle {
            Saksbehandler,
            Søker,
        }
    }

    fun meldekortInnsendt(
        ident: String,
        meldekortId: UUID,
        meldeperiode: Periode,
        korrigeringAv: UUID? = null,
        korrigertAv: MeldekortKilde,
        aktiviteter: List<MeldekortAktivitet> = emptyList(),
        meldedato: LocalDate = meldeperiode.tilOgMed.plusDays(1),
        meldtITide: Boolean = true,
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
                                        "meldt" to meldtITide,
                                        "aktiviteter" to
                                            when (aktivitet) {
                                                is MeldekortAktivitet.Arbeid -> {
                                                    listOf(mapOf("type" to "Arbeid", "timer" to aktivitet.timer.somIsoVarighet()))
                                                }

                                                is MeldekortAktivitet.Syk -> {
                                                    listOf(mapOf("type" to "Syk"))
                                                }

                                                is MeldekortAktivitet.Fravær -> {
                                                    listOf(mapOf("type" to "Fravaer"))
                                                }

                                                is MeldekortAktivitet.Utdanning -> {
                                                    listOf(mapOf("type" to "Utdanning", "timer" to aktivitet.timer.somIsoVarighet()))
                                                }

                                                is MeldekortAktivitet.IngenAktivitet -> {
                                                    emptyList<Map<String, Any>>()
                                                }
                                            },
                                    )
                                },
                            "kilde" to
                                mapOf(
                                    "rolle" to korrigertAv.kilde.name,
                                    "ident" to korrigertAv.ident,
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

    private fun Double.somIsoVarighet(): String {
        val totalMinutter = (this * 60).roundToInt()
        val heleTimer = totalMinutter / 60
        val minutter = totalMinutter % 60
        return buildString {
            append("PT")
            if (heleTimer > 0) append("${heleTimer}H")
            if (minutter > 0) append("${minutter}M")
            if (heleTimer == 0 && minutter == 0) append("0S")
        }
    }

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
