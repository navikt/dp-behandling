package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.behandling.modell.hendelser.AktivitetType
import no.nav.dagpenger.behandling.modell.hendelser.Dag
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortAktivitet
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortId
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.regel.Behov.AndreØkonomiskeYtelser
import no.nav.dagpenger.regel.Behov.Barnetillegg
import no.nav.dagpenger.regel.Behov.BostedslandErNorge
import no.nav.dagpenger.regel.Behov.Foreldrepenger
import no.nav.dagpenger.regel.Behov.HelseTilAlleTyperJobb
import no.nav.dagpenger.regel.Behov.Inntekt
import no.nav.dagpenger.regel.Behov.KanJobbeDeltid
import no.nav.dagpenger.regel.Behov.KanJobbeHvorSomHelst
import no.nav.dagpenger.regel.Behov.Lønnsgaranti
import no.nav.dagpenger.regel.Behov.Omsorgspenger
import no.nav.dagpenger.regel.Behov.OppgittAndreYtelserUtenforNav
import no.nav.dagpenger.regel.Behov.Opplæringspenger
import no.nav.dagpenger.regel.Behov.Ordinær
import no.nav.dagpenger.regel.Behov.Permittert
import no.nav.dagpenger.regel.Behov.PermittertFiskeforedling
import no.nav.dagpenger.regel.Behov.Pleiepenger
import no.nav.dagpenger.regel.Behov.RegistrertSomArbeidssøker
import no.nav.dagpenger.regel.Behov.Svangerskapspenger
import no.nav.dagpenger.regel.Behov.Sykepenger
import no.nav.dagpenger.regel.Behov.Søknadsdato
import no.nav.dagpenger.regel.Behov.TarUtdanningEllerOpplæring
import no.nav.dagpenger.regel.Behov.Verneplikt
import no.nav.dagpenger.regel.Behov.VilligTilÅBytteYrke
import no.nav.dagpenger.regel.Behov.ØnskerDagpengerFraDato
import no.nav.dagpenger.regel.Behov.ØnsketArbeidstid
import no.nav.dagpenger.uuid.UUIDv7
import org.postgresql.util.PGobject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.time.Duration.Companion.hours

class TestPerson(
    internal val ident: String,
    private val rapid: TestRapid,
    internal val søknadsdato: LocalDate = 5.mai(2021),
    val alder: Int = 30,
    private val innsendt: LocalDateTime = LocalDateTime.now(),
    var InntektSiste12Mnd: Int = 1234,
    val InntektSiste36Mnd: Int = 1234,
    internal var ønskerFraDato: LocalDate = søknadsdato,
    var arbeidssøkerregistreringsdato: LocalDate = søknadsdato,
    var registrertSomArbeidssøker: Boolean = true,
    var søkerVerneplikt: Boolean = false,
    val ordinær: Boolean = true,
    val permittering: Boolean = false,
    val fiskepermittering: Boolean = false,
) {
    val inntektId = "01HQTE3GBWCSVYH6S436DYFREN"
    internal val søknadId = "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
    internal val behandlingId by lazy { rapid.inspektør.field(1, "behandlingId").asText() }
    private val meldekortRepository = MeldekortRepositoryPostgres()

    fun sendSøknad() = rapid.sendTestMessage(søknadInnsendt(), ident)

    private fun søknadInnsendt() =
        JsonMessage
            .newMessage(
                "søknad_behandlingsklar",
                mapOf(
                    "innsendt" to innsendt,
                    "ident" to ident,
                    "fagsakId" to 123,
                    "bruk-dp-behandling" to true,
                    "søknadId" to søknadId,
                ),
            ).toJson()

    fun løsBehov(vararg behov: String) {
        val behovSomLøses = løsninger.filterKeys { it in behov }
        require(behovSomLøses.size == behov.size) { "Fant ikke løsning for alle behov: $behov" }
        rapid.sendTestMessage(løstBehov(behovSomLøses), ident)
    }

    fun endreOpplysning(
        behov: String,
        løsning: Any,
        data: Map<String, Any> = emptyMap(),
    ) {
        val message =
            JsonMessage
                .newMessage(
                    "behov",
                    mapOf(
                        "ident" to ident,
                        "behandlingId" to behandlingId,
                        "søknadId" to søknadId,
                        "@behov" to listOf(behov),
                        "@opplysningsbehov" to true,
                        "@final" to true,
                        "@løsning" to
                            mapOf(
                                behov to løsning,
                            ),
                    ) + data,
                ).toJson()

        rapid.sendTestMessage(message, ident)
    }

    fun løsBehov(
        behov: String,
        løsning: Any,
        data: Map<String, Any> = emptyMap(),
    ) {
        rapid.sendTestMessage(løstBehov(mapOf(behov to løsning), true, data), ident)
    }

    private fun løstBehov(
        løsninger: Map<String, Any>,
        opplysningsbehov: Boolean = true,
        data: Map<String, Any> = emptyMap(),
    ): String =
        rapid.inspektør.message(rapid.inspektør.size - 1).run {
            val løsningsobjekt = this as ObjectNode
            løsningsobjekt.put("@final", true)
            løsningsobjekt.putPOJO("@løsning", løsninger)
            objectMapper.writeValueAsString(løsningsobjekt)
        }

    private val objectMapper =
        jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun markerAvklaringerIkkeRelevant(avklaringer: Map<String, String>) {
        avklaringer.forEach { (id, kode) ->
            markerAvklaringIkkeRelevant(id, kode)
        }
    }

    fun markerAvklaringIkkeRelevant(
        avklaringId: String,
        kode: String,
    ) {
        rapid.sendTestMessage(avklaringIkkeRelevant(avklaringId, kode), ident)
    }

    private fun avklaringIkkeRelevant(
        avklaringId: String,
        kode: String,
    ) = JsonMessage
        .newMessage(
            "AvklaringIkkeRelevant",
            mapOf(
                "ident" to ident,
                "behandlingId" to behandlingId,
                "avklaringId" to avklaringId,
                "kode" to kode,
            ),
        ).toJson()

    fun avbrytBehandling() {
        rapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    "avbryt_behandling",
                    mapOf(
                        "behandlingId" to behandlingId,
                        "ident" to ident,
                    ),
                ).toJson(),
            ident,
        )
    }

    fun sendTilKontroll() {
        rapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    "oppgave_sendt_til_kontroll",
                    mapOf(
                        "behandlingId" to behandlingId,
                        "ident" to ident,
                    ),
                ).toJson(),
            ident,
        )
    }

    fun returnerTilSaksbehandler() {
        rapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    "oppgave_returnert_til_saksbehandling",
                    mapOf(
                        "behandlingId" to behandlingId,
                        "ident" to ident,
                    ),
                ).toJson(),
            ident,
        )
    }

    fun sendMeldekort(
        start: LocalDate,
        løpenummer: Long,
        arbeidstimerPerDag: Int = 1,
        korrigeringAv: Long? = null,
    ): UUID {
        val meldekortId = UUIDv7.ny()
        val meldekort =
            Meldekort(
                id = meldekortId,
                eksternMeldekortId = MeldekortId(løpenummer.toString()),
                meldingsreferanseId = UUIDv7.ny(),
                ident = ident,
                fom = start,
                tom = start.plusDays(13),
                kilde = MeldekortKilde("Bruker", ident),
                dager =
                    (0..<14).map {
                        Dag(
                            dato = start.plusDays(it.toLong()),
                            meldt = true,
                            aktiviteter = listOf(MeldekortAktivitet(type = AktivitetType.Arbeid, timer = arbeidstimerPerDag.hours)),
                        )
                    },
                innsendtTidspunkt = 14.juni(2021).atStartOfDay(),
                originalMeldekortId = korrigeringAv?.let { MeldekortId(it.toString()) },
            )
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO melding (ident, melding_id, melding_type, data, lest_dato)
                    VALUES (:ident, :melding_id, :melding_type, :data, NOW())
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident,
                        "melding_id" to meldekort.meldingsreferanseId,
                        "melding_type" to "Meldekort",
                        "data" to
                            PGobject().apply {
                                type = "json"
                                value = "{}"
                            },
                        "opprettet" to LocalDateTime.now(),
                    ),
                ).asUpdate,
            )
        }

        meldekortRepository.lagre(meldekort)
        return meldekortId
    }

    fun beregnMeldekort(meldekortId: UUID) {
        rapid
            .sendTestMessage(
                JsonMessage
                    .newMessage(
                        "beregn_meldekort",
                        mapOf(
                            "meldekortId" to meldekortId,
                            "ident" to ident,
                        ),
                    ).toJson(),
                ident,
            )
    }

    private val inntektV1
        get() =
            no.nav.dagpenger.inntekt.v1.Inntekt(
                inntektsId = "01J677GHJRC2H08Q55DASFD0XX",
                inntektsListe =
                    listOf(
                        KlassifisertInntektMåned(
                            årMåned = YearMonth.from(søknadsdato.minusMonths(2)),
                            klassifiserteInntekter =
                                listOf(
                                    no.nav.dagpenger.inntekt.v1.KlassifisertInntekt(
                                        beløp = InntektSiste12Mnd.toBigDecimal(),
                                        inntektKlasse = no.nav.dagpenger.inntekt.v1.InntektKlasse.ARBEIDSINNTEKT,
                                    ),
                                    no.nav.dagpenger.inntekt.v1.KlassifisertInntekt(
                                        beløp = 50.toBigDecimal(),
                                        inntektKlasse = no.nav.dagpenger.inntekt.v1.InntektKlasse.SYKEPENGER,
                                    ),
                                ),
                            harAvvik = false,
                        ),
                    ),
                sisteAvsluttendeKalenderMåned = YearMonth.from(søknadsdato.minusMonths(2)),
            )

    private val løsninger
        get() =
            mapOf(
                "Fødselsdato" to søknadsdato.minusYears(alder.toLong()),
                Søknadsdato to søknadsdato,
                ØnskerDagpengerFraDato to ønskerFraDato,
                ØnsketArbeidstid to 40.0,
                // Inntekt
                Inntekt to mapOf("verdi" to inntektV1),
                // Reell arbeidssøker
                KanJobbeDeltid to true,
                KanJobbeHvorSomHelst to true,
                HelseTilAlleTyperJobb to true,
                VilligTilÅBytteYrke to true,
                // Arbeidssøkerregistrering
                RegistrertSomArbeidssøker to
                    mapOf(
                        "verdi" to registrertSomArbeidssøker,
                        "gyldigFraOgMed" to arbeidssøkerregistreringsdato,
                    ) +
                    if (!registrertSomArbeidssøker) {
                        mapOf("gyldigTilOgMed" to arbeidssøkerregistreringsdato)
                    } else {
                        emptyMap()
                    },
                // Rettighetsype
                Ordinær to ordinær,
                Permittert to permittering,
                Lønnsgaranti to false,
                PermittertFiskeforedling to fiskepermittering,
                // Verneplikt
                Verneplikt to søkerVerneplikt,
                TarUtdanningEllerOpplæring to false,
                BostedslandErNorge to true,
                Barnetillegg to
                    mapOf(
                        "verdi" to
                            listOf<Map<String, Any>>(
                                mapOf(
                                    "fødselsdato" to 1.januar(2000),
                                    "kvalifiserer" to true,
                                ),
                            ),
                    ),
                "Beregnet vanlig arbeidstid per uke før tap" to 40,
                Sykepenger to false,
                Omsorgspenger to false,
                Svangerskapspenger to false,
                Foreldrepenger to false,
                Opplæringspenger to false,
                Pleiepenger to false,
                OppgittAndreYtelserUtenforNav to false,
                AndreØkonomiskeYtelser to false,
            )
}
