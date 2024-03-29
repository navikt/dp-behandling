package no.nav.dagpenger.behandling

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.time.LocalDate

class TestPerson(
    private val ident: String,
    private val rapid: TestRapid,
    private val søknadstidspunkt: LocalDate = 5.mai(2021),
    private val alder: Int = 30,
) {
    val inntektId = "01HQTE3GBWCSVYH6S436DYFREN"
    internal val søknadId = "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"
    private val behandlingId by lazy { rapid.inspektør.field(1, "behandlingId").asText() }

    fun sendSøknad() = rapid.sendTestMessage(søknadInnsendt())

    private fun søknadInnsendt() =
        JsonMessage.newMessage(
            "innsending_ferdigstilt",
            mapOf(
                "type" to "NySøknad",
                "fødselsnummer" to ident,
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

    private fun løstBehov(løsninger: Map<String, Any>) =
        JsonMessage.newMessage(
            "behov",
            mapOf(
                "ident" to ident,
                "behandlingId" to behandlingId,
                "søknadId" to søknadId,
                "@final" to true,
                "@løsning" to løsninger,
            ),
        ).toJson()

    private val løsninger =
        mapOf(
            "Fødselsdato" to søknadstidspunkt.minusYears(alder.toLong()),
            "Søknadstidspunkt" to søknadstidspunkt,
            "ØnskerDagpengerFraDato" to søknadstidspunkt,
            "InntektId" to mapOf("verdi" to inntektId),
            "InntektSiste12Mnd" to 1234,
            "InntektSiste36Mnd" to 1234,
        )
}
