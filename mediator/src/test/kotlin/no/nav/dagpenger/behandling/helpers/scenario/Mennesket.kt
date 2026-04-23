package no.nav.dagpenger.behandling.helpers.scenario

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.behandling.januar
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.inntekt.v1.Inntekt
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.opplysning.verdier.Periode
import no.nav.dagpenger.regel.Behov
import no.nav.dagpenger.regel.Behov.BostedslandErNorge
import no.nav.dagpenger.uuid.UUIDv7
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random

internal class Mennesket(
    private val rapid: TestRapid,
    private val scenario: SimulertDagpengerSystem.ScenarioOptions,
) {
    val ident = scenario.ident
    private val alder = scenario.alder
    private val inntektSiste12Mnd = scenario.inntektSiste12Mnd

    private val søknader = mutableListOf<UUID>()
    private val fagsak = mutableListOf<Int>()
    private lateinit var søknadsdato: LocalDate
    private lateinit var ønskerFraDato: LocalDate
    private lateinit var meldesyklus: Meldesyklus
    val arbeidsøkerRegisterPerioder = mutableListOf<Periode>()

    val sisteSøknadId get() = søknader.lastOrNull()

    fun søkDagpenger(
        dato: LocalDate = LocalDate.now(),
        ønskerFraDato: LocalDate = dato,
    ) {
        this.søknadsdato = dato
        this.ønskerFraDato = ønskerFraDato
        this.meldesyklus = Meldesyklus(søknadsdato)
        arbeidsøkerRegisterPerioder.add(Periode(dato, LocalDate.MAX))

        rapid.sendTestMessage(
            Meldingskatalog.søknadInnsendt(
                ident = ident,
                innsendt = søknadsdato.atStartOfDay(),
                fagsakId = fagsak.ny(),
                søknadId = søknader.ny(),
            ),
            ident,
        )
    }

    fun søkGjenopptak(
        dato: LocalDate = LocalDate.now(),
        ønskerFraDato: LocalDate = dato,
    ): UUID {
        this.søknadsdato = dato
        this.ønskerFraDato = ønskerFraDato
        val nySøknadId = søknader.ny()
        rapid.sendTestMessage(
            Meldingskatalog.søknadInnsendt(
                ident = ident,
                innsendt = søknadsdato.atStartOfDay(),
                fagsakId = 0,
                søknadId = nySøknadId,
            ),
            ident,
        )
        return nySøknadId
    }

    fun løsningFor(behov: Map<String, JsonNode>): Map<String, Any> {
        val behovsløsning =
            behov.keys.associateWith { behovNavn ->
                val løser = løsninger[behovNavn] ?: error("Fant ikke løsning for behov $behovNavn")
                val behovsMelding = behov[behovNavn] ?: error("Fant ikke melding for behov $behovNavn")
                løser.løs(behovsMelding)
            }

        return behovsløsning
    }

    val behandlingId: UUID
        get() {
            for (offset in rapid.inspektør.size - 1 downTo 0) {
                val message = rapid.inspektør.message(offset)
                if (message["@event_name"].asText() == "behandling_opprettet") {
                    return message["behandlingId"].asUUID()
                }
            }
            throw NoSuchElementException("Fant ingen behandling_opprettet-melding")
        }

    val behandling get() = behandling(behandlingId)

    fun behandling(behandlingId: UUID): BehandlingsresultatDTO {
        for (offset in rapid.inspektør.size - 1 downTo 0) {
            val message = rapid.inspektør.message(offset)
            if (message["@event_name"].asText() == "forslag_til_behandlingsresultat" &&
                message["behandlingId"].asUUID() == behandlingId
            ) {
                return objectMapper.convertValue(message, BehandlingsresultatDTO::class.java)
            }
        }
        throw NoSuchElementException("Fant ingen behandling med UUID=$behandlingId")
    }

    fun opprettBehandling(fraDato: LocalDate) {
        rapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    "opprett_behandling",
                    mapOf("ident" to ident, "prøvingsdato" to fraDato),
                ).toJson(),
        )
    }

    fun sendInnMeldekort(
        nummer: Int,
        korrigeringAv: UUID? = null,
        timer: List<Int> = emptyList(),
    ): UUID = sendInnMeldekort(meldesyklus.periode(nummer), korrigeringAv, timer)

    fun fastsattMeldedato(nummer: Int) = meldesyklus.periode(nummer).fraOgMed

    fun sendInnMeldekort(
        periode: Periode,
        korrigeringAv: UUID? = null,
        timer: List<Int> = emptyList(),
    ): UUID {
        val meldekortId = UUIDv7.ny()
        val message = Meldingskatalog.meldekortInnsendt(ident, meldekortId, periode, korrigeringAv, timer)
        rapid.sendTestMessage(message)
        return meldekortId
    }

    fun avsluttArbeidssøkerperiode(
        fastsattMeldingsdag: LocalDate,
        avsluttetTidspunkt: LocalDateTime = LocalDateTime.now(),
        fristBrutt: Boolean = false,
        manueltAvregistrert: Boolean = false,
    ) {
        val message =
            Meldingskatalog.avsluttArbeidssøkerperiode(
                ident = ident,
                fastsattMeldingsdag,
                avsluttetTidspunkt,
                fristBrutt,
                manueltAvregistrert,
            )

        rapid.sendTestMessage(message)
    }

    val avklaringer: List<Avklaring>
        get() {
            val liste = mutableListOf<Avklaring>()
            for (offset in rapid.inspektør.size - 1 downTo 0) {
                val message = rapid.inspektør.message(offset)
                if (message["@event_name"].asText() == "NyAvklaring") {
                    liste.add(Avklaring(message["avklaringId"].asUUID(), message["kode"].asText()))
                }
            }
            return liste.toList()
        }

    internal data class Avklaring(
        val id: UUID,
        val kode: String,
    )

    private val løsninger: Map<String, Behovsløsning>
        get() =
            mapOf(
                "Fødselsdato" to Behovsløsning.Statisk(søknadsdato.minusYears(alder.toLong())),
                Behov.Søknadsdato to Behovsløsning.Statisk(søknadsdato),
                Behov.ØnskerDagpengerFraDato to Behovsløsning.Statisk(ønskerFraDato),
                Behov.ØnsketArbeidstid to Behovsløsning.Statisk(40.0),
                // Inntekt
                Behov.Inntekt to Behovsløsning.Statisk(mapOf("verdi" to inntektV1)),
                // Reell arbeidssøker
                Behov.KanJobbeDeltid to Behovsløsning.Statisk(true),
                Behov.KanJobbeHvorSomHelst to Behovsløsning.Statisk(true),
                Behov.HelseTilAlleTyperJobb to Behovsløsning.Statisk(true),
                Behov.VilligTilÅBytteYrke to Behovsløsning.Statisk(true),
                // Simulerer dp-oppslag-arbeidssoker
                Behov.RegistrertSomArbeidssøker to
                    Behovsløsning.FraBehov { melding ->
                        val prøvingsdato = LocalDate.parse(melding[Behov.RegistrertSomArbeidssøker][Behov.Prøvingsdato].asText())
                        val periode = arbeidsøkerRegisterPerioder.lastOrNull { periode -> prøvingsdato in periode }
                        if (periode != null) {
                            mapOf(
                                "verdi" to true,
                                "gyldigFraOgMed" to maxOf(periode.fraOgMed, prøvingsdato),
                            )
                        } else {
                            mapOf(
                                "verdi" to false,
                                "gyldigFraOgMed" to prøvingsdato,
                                "gyldigTilOgMed" to arbeidsøkerRegisterPerioder.first().fraOgMed.minusDays(1),
                            )
                        }
                    },
                // Rettighetstype
                Behov.Ordinær to Behovsløsning.Statisk(scenario.ordinær),
                Behov.Permittert to Behovsløsning.Statisk(scenario.permittering),
                Behov.Lønnsgaranti to Behovsløsning.Statisk(false),
                Behov.PermittertFiskeforedling to Behovsløsning.Statisk(scenario.permittertfraFiskeforedling),
                // Verneplikt
                Behov.Verneplikt to Behovsløsning.Statisk(scenario.verneplikt),
                BostedslandErNorge to Behovsløsning.Statisk(true),
                Behov.TarUtdanningEllerOpplæring to Behovsløsning.Statisk(false),
                Behov.BarnetilleggV2 to
                    Behovsløsning.Statisk(
                        mapOf(
                            "verdi" to
                                mapOf(
                                    "søknadbarnId" to UUIDv7.ny(),
                                    "barn" to
                                        listOf(
                                            mapOf(
                                                "fødselsdato" to 1.januar(2000),
                                                "kvalifiserer" to true,
                                            ),
                                        ),
                                ),
                        ),
                    ),
                "Beregnet vanlig arbeidstid per uke før tap" to Behovsløsning.Statisk(40),
                Behov.Sykepenger to Behovsløsning.Statisk(false),
                Behov.Omsorgspenger to Behovsløsning.Statisk(false),
                Behov.Svangerskapspenger to Behovsløsning.Statisk(false),
                Behov.Foreldrepenger to Behovsløsning.Statisk(false),
                Behov.Opplæringspenger to Behovsløsning.Statisk(false),
                Behov.Pleiepenger to Behovsløsning.Statisk(false),
                Behov.OppgittAndreYtelserUtenforNav to Behovsløsning.Statisk(false),
                Behov.AndreØkonomiskeYtelser to Behovsløsning.Statisk(false),
                Behov.Uføre to Behovsløsning.Statisk(false),
                Behov.AntallDagerForbukt to
                    Behovsløsning.Statisk(
                        mapOf(
                            "verdi" to 100,
                            "gyldigFraOgMed" to LocalDate.of(søknadsdato.year, 1, 1),
                            "gyldigTilOgMed" to LocalDate.of(søknadsdato.year, 12, 31),
                        ),
                    ),
                Behov.OpptjeningsBeløp to
                    Behovsløsning.Statisk(
                        mapOf(
                            "verdi" to inntektSiste12Mnd,
                            "gyldigFraOgMed" to LocalDate.of(søknadsdato.year, 1, 1),
                            "gyldigTilOgMed" to LocalDate.of(søknadsdato.year, 12, 31),
                        ),
                    ),
            )

    internal sealed class Behovsløsning {
        abstract fun løs(behovMelding: JsonNode): Any

        class Statisk(
            private val verdi: Any,
        ) : Behovsløsning() {
            override fun løs(behovMelding: JsonNode): Any = verdi
        }

        class FraBehov(
            private val resolver: (JsonNode) -> Any,
        ) : Behovsløsning() {
            override fun løs(behovMelding: JsonNode): Any = resolver(behovMelding)
        }
    }

    private val inntektV1 get() = inntekt(inntektSiste12Mnd.toBigDecimal(), søknadsdato.minusMonths(2))

    fun inntekt(
        inntekt: Int,
        fraOgMed: LocalDate,
    ) = inntekt(inntekt.toBigDecimal(), fraOgMed)

    fun inntekt(
        inntekt: BigDecimal,
        fraOgMed: LocalDate,
    ): Inntekt =
        Inntekt(
            inntektsId = "01J677GHJRC2H08Q55DASFD0XX",
            inntektsListe =
                listOf(
                    KlassifisertInntektMåned(
                        årMåned = YearMonth.from(fraOgMed),
                        klassifiserteInntekter =
                            listOf(
                                KlassifisertInntekt(
                                    beløp = inntekt,
                                    inntektKlasse = InntektKlasse.ARBEIDSINNTEKT,
                                ),
                            ),
                        harAvvik = false,
                    ),
                ),
            sisteAvsluttendeKalenderMåned = YearMonth.from(fraOgMed),
        )

    private companion object {
        fun MutableList<UUID>.ny() = UUID.randomUUID().also { add(it) }

        fun MutableList<UUID>.siste() = last()

        fun MutableList<Int>.ny() = Random.nextInt().also { add(it) }

        fun MutableList<Int>.siste() = last()
    }
}
