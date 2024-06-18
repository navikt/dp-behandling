package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.modell.BehandlingBehov.AvklaringManuellBehandling
import no.nav.dagpenger.regel.Behov.HelseTilAlleTyperJobb
import no.nav.dagpenger.regel.Behov.InntektId
import no.nav.dagpenger.regel.Behov.KanJobbeDeltid
import no.nav.dagpenger.regel.Behov.KanJobbeHvorSomHelst
import no.nav.dagpenger.regel.Behov.Lønnsgaranti
import no.nav.dagpenger.regel.Behov.Ordinær
import no.nav.dagpenger.regel.Behov.Permittert
import no.nav.dagpenger.regel.Behov.PermittertFiskeforedling
import no.nav.dagpenger.regel.Behov.RegistrertSomArbeidssøker
import no.nav.dagpenger.regel.Behov.TarUtdanningEllerOpplæring
import no.nav.dagpenger.regel.Behov.Verneplikt
import no.nav.dagpenger.regel.Behov.VilligTilÅBytteYrke
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.time.LocalDate
import java.time.LocalDateTime

class TestPerson(
    private val ident: String,
    private val rapid: TestRapid,
    internal val søknadstidspunkt: LocalDate = 5.mai(2021),
    private val alder: Int = 30,
    private val innsendt: LocalDateTime = LocalDateTime.now(),
) {
    val inntektId = "01HQTE3GBWCSVYH6S436DYFREN"
    internal val søknadId = "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
    private val behandlingId by lazy { rapid.inspektør.field(1, "behandlingId").asText() }

    fun sendSøknad() = rapid.sendTestMessage(søknadInnsendt())

    private fun søknadInnsendt() =
        JsonMessage.newMessage(
            "innsending_ferdigstilt",
            mapOf(
                "@opprettet" to innsendt,
                "type" to "NySøknad",
                "fødselsnummer" to ident,
                "fagsakId" to 123,
                "bruk-dp-behandling" to true,
                "søknadsData" to
                    mapOf(
                        "søknad_uuid" to søknadId,
                    ),
            ),
        ).toJson()

    fun løsBehov(vararg behov: String) {
        val behovSomLøses = løsninger.filterKeys { it in behov }
        require(behovSomLøses.size == behov.size) { "Fant ikke løsning for alle behov: $behov" }
        rapid.sendTestMessage(løstBehov(behovSomLøses))
    }

    fun løsBehov(
        behov: String,
        løsning: Any,
        data: Map<String, Any> = emptyMap(),
    ) {
        rapid.sendTestMessage(løstBehov(mapOf(behov to løsning), false, data))
    }

    private fun løstBehov(
        løsninger: Map<String, Any>,
        opplysningsbehov: Boolean = true,
        data: Map<String, Any> = emptyMap(),
    ) = JsonMessage.newMessage(
        "behov",
        mapOf(
            "ident" to ident,
            "behandlingId" to behandlingId,
            "søknadId" to søknadId,
            "@opplysningsbehov" to opplysningsbehov,
            "@behov" to løsninger.keys.toList(),
            "@final" to true,
            "@løsning" to løsninger,
        ) + data,
    ).toJson()

    fun avbrytBehandling() {
        rapid.sendTestMessage(
            JsonMessage.newMessage(
                "avbryt_behandling",
                mapOf(
                    "behandlingId" to behandlingId,
                    "ident" to ident,
                ),
            ).toJson(),
        )
    }

    private val løsninger =
        mapOf(
            "Fødselsdato" to søknadstidspunkt.minusYears(alder.toLong()),
            "Søknadstidspunkt" to søknadstidspunkt,
            "ØnskerDagpengerFraDato" to søknadstidspunkt,
            // Inntekt
            InntektId to mapOf("verdi" to inntektId),
            "InntektSiste12Mnd" to 1234,
            "InntektSiste36Mnd" to 1234,
            // Reell arbeidssøker
            KanJobbeDeltid to true,
            KanJobbeHvorSomHelst to true,
            HelseTilAlleTyperJobb to true,
            VilligTilÅBytteYrke to true,
            // Arbeidssøkerregistrering
            RegistrertSomArbeidssøker to LocalDate.now().minusDays(1),
            // Rettighetsype
            Ordinær to false,
            Permittert to true,
            Lønnsgaranti to false,
            PermittertFiskeforedling to false,
            AvklaringManuellBehandling.name to false,
            // Verneplikt
            Verneplikt to false,
            TarUtdanningEllerOpplæring to false,
        )
}
