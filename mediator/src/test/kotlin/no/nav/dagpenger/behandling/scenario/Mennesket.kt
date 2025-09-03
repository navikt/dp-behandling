package no.nav.dagpenger.behandling.scenario

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.dagpenger.behandling.api.models.VedtakDTO
import no.nav.dagpenger.behandling.januar
import no.nav.dagpenger.behandling.mediator.asUUID
import no.nav.dagpenger.behandling.objectMapper
import no.nav.dagpenger.inntekt.v1.InntektKlasse
import no.nav.dagpenger.inntekt.v1.KlassifisertInntekt
import no.nav.dagpenger.inntekt.v1.KlassifisertInntektMåned
import no.nav.dagpenger.regel.Behov
import no.nav.dagpenger.regel.Behov.BostedslandErNorge
import no.nav.dagpenger.uuid.UUIDv7
import java.time.LocalDate
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

    fun søkDagpenger(
        dato: LocalDate = LocalDate.now(),
        ønskerFraDato: LocalDate = dato,
    ) {
        this.søknadsdato = dato
        this.ønskerFraDato = ønskerFraDato
        this.meldesyklus = Meldesyklus(søknadsdato)

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

    fun løsningFor(behov: List<String>): Map<String, Any> {
        val behovSomLøses = løsninger.filterKeys { it in behov }
        require(behovSomLøses.size == behov.toSet().size) { "Fant ikke løsning for alle behov: $behov" }
        return behovSomLøses
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

    fun behandling(behandlingId: UUID): VedtakDTO {
        for (offset in rapid.inspektør.size - 1 downTo 0) {
            val message = rapid.inspektør.message(offset)
            if (message["@event_name"].asText() == "forslag_til_vedtak" &&
                message["behandlingId"].asUUID() == behandlingId
            ) {
                return objectMapper.convertValue(message, VedtakDTO::class.java)
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

    fun sendInnMeldekort(nummer: Int) {
        val meldekortId = UUIDv7.ny()
        val message = Meldingskatalog.sendMeldekort(ident, meldekortId, meldesyklus.periode(nummer))
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

    private val løsninger
        get() =
            mapOf(
                "Fødselsdato" to søknadsdato.minusYears(alder.toLong()),
                Behov.Søknadsdato to søknadsdato,
                Behov.ØnskerDagpengerFraDato to ønskerFraDato,
                Behov.ØnsketArbeidstid to 40.0,
                // Inntekt
                Behov.Inntekt to mapOf("verdi" to inntektV1),
                // Reell arbeidssøker
                Behov.KanJobbeDeltid to true,
                Behov.KanJobbeHvorSomHelst to true,
                Behov.HelseTilAlleTyperJobb to true,
                Behov.VilligTilÅBytteYrke to true,
                // Arbeidssøkerregistrering
                Behov.RegistrertSomArbeidssøker to
                    mapOf(
                        "verdi" to true,
                        // "gyldigFraOgMed" to arbeidssøkerregistreringsdato,
                        // "gyldigTilOgMed" to arbeidssøkerregistreringsdato,
                    ),
                // Rettighetsype
                Behov.Ordinær to scenario.ordinær,
                Behov.Permittert to scenario.permittering,
                Behov.Lønnsgaranti to false,
                Behov.PermittertFiskeforedling to false,
                // Verneplikt
                Behov.Verneplikt to false,
                BostedslandErNorge to true,
                Behov.TarUtdanningEllerOpplæring to false,
                Behov.Barnetillegg to
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
                Behov.Sykepenger to false,
                Behov.Omsorgspenger to false,
                Behov.Svangerskapspenger to false,
                Behov.Foreldrepenger to false,
                Behov.Opplæringspenger to false,
                Behov.Pleiepenger to false,
                Behov.OppgittAndreYtelserUtenforNav to false,
                Behov.AndreØkonomiskeYtelser to false,
            )

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
                                    KlassifisertInntekt(
                                        beløp = inntektSiste12Mnd.toBigDecimal(),
                                        inntektKlasse = InntektKlasse.ARBEIDSINNTEKT,
                                    ),
                                ),
                            harAvvik = false,
                        ),
                    ),
                sisteAvsluttendeKalenderMåned = YearMonth.from(søknadsdato.minusMonths(2)),
            )

    private companion object {
        fun MutableList<UUID>.ny() = UUID.randomUUID().also { add(it) }

        fun MutableList<UUID>.siste() = last()

        fun MutableList<Int>.ny() = Random.nextInt().also { add(it) }

        fun MutableList<Int>.siste() = last()
    }
}
