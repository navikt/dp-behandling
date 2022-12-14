package no.nav.dagpenger.behandling

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.mockk
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.Aldersbehov
import no.nav.dagpenger.behandling.Aktivitetslogg.Aktivitet.Behov.Behovtype.VedtakInnvilgetBehov
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

internal class BehovMediatorTest {
    private companion object {
        private const val testIdent = "12345678912"
        private lateinit var behovMediator: BehovMediator
    }

    private val testRapid = TestRapid()
    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var testPersonKontekst: TestPersonKontekst

    @BeforeEach
    fun setup() {
        aktivitetslogg = Aktivitetslogg()
        testPersonKontekst = TestPersonKontekst(testIdent)
        behovMediator = BehovMediator(
            rapidsConnection = testRapid,
            sikkerLogg = mockk(relaxed = true)
        )
        testRapid.reset()
    }

    @Test
    internal fun `Behov blir sendt og inneholder det den skal`() {
        val hendelse = TestHendelse(aktivitetslogg.barn())
        hendelse.kontekst(testPersonKontekst)
        hendelse.kontekst(Testkontekst("Testkontekst"))

        hendelse.behov(
            Aldersbehov,
            "Behøver tom søknad for denne søknaden",
            mapOf(
                "parameter1" to "verdi1",
                "parameter2" to "verdi2"
            )
        )

        behovMediator.håndter(hendelse)

        val inspektør = testRapid.inspektør

        assertEquals(1, inspektør.size)
        assertEquals(testIdent, inspektør.key(0), "Forventer at partisjonsnøkker er ident ($testIdent)")
        inspektør.message(0).also { json ->
            assertStandardBehovFelter(json)
            assertEquals(listOf("Aldersbehov"), json["@behov"].map(JsonNode::asText))
            assertEquals(testIdent, json["ident"].asText())
            assertEquals("Testkontekst", json["Testkontekst"].asText())
            assertEquals("verdi1", json["parameter1"].asText())
            assertEquals("verdi2", json["parameter2"].asText())
            assertEquals("verdi1", json["Aldersbehov"]["parameter1"].asText())
            assertEquals("verdi2", json["Aldersbehov"]["parameter2"].asText())
        }
    }

    @Test
    internal fun `Gruppere behov`() {
        val hendelse = TestHendelse(aktivitetslogg.barn())
        hendelse.kontekst(testPersonKontekst)
        hendelse.kontekst(Testkontekst("Testkontekst"))

        hendelse.behov(
            Aldersbehov,
            "Trenger søknad på et arkiverbart format",
            mapOf(
                "parameter1" to "verdi1",
                "parameter2" to "verdi2"
            )
        )

        hendelse.behov(
            VedtakInnvilgetBehov,
            "Behøver tom søknad for denne søknaden",
            mapOf(
                "parameter3" to "verdi3",
                "parameter4" to "verdi4"
            )
        )

        behovMediator.håndter(hendelse)

        val inspektør = testRapid.inspektør

        assertEquals(1, inspektør.size)
        inspektør.message(0).also { json ->
            assertStandardBehovFelter(json)
            assertEquals(listOf("Aldersbehov", "VedtakInnvilgetBehov"), json["@behov"].map(JsonNode::asText))
            assertEquals(testIdent, json["ident"].asText())
            assertEquals("Testkontekst", json["Testkontekst"].asText())
            assertEquals("verdi1", json["parameter1"].asText())
            assertEquals("verdi2", json["parameter2"].asText())
            assertEquals("verdi3", json["parameter3"].asText())
            assertEquals("verdi4", json["parameter4"].asText())
            assertEquals("verdi1", json["Aldersbehov"]["parameter1"].asText())
            assertEquals("verdi2", json["Aldersbehov"]["parameter2"].asText())
            assertEquals("verdi3", json["VedtakInnvilgetBehov"]["parameter3"].asText())
            assertEquals("verdi4", json["VedtakInnvilgetBehov"]["parameter4"].asText())
        }
    }

    @Test
    internal fun `sjekker etter duplikatverdier`() {
        val hendelse = TestHendelse(aktivitetslogg.barn())
        hendelse.kontekst(testPersonKontekst)
        hendelse.behov(
            Aldersbehov,
            "Behøver tom søknad for denne søknaden",
            mapOf(
                "ident" to testIdent
            )
        )
        hendelse.behov(
            Aldersbehov,
            "Behøver tom søknad for denne søknaden",
            mapOf(
                "ident" to testIdent
            )
        )

        assertThrows<IllegalArgumentException> { behovMediator.håndter(hendelse) }
    }

    @Test
    internal fun `kan ikke produsere samme behov`() {
        val hendelse = TestHendelse(aktivitetslogg.barn())
        hendelse.kontekst(testPersonKontekst)
        hendelse.behov(Aldersbehov, "Behøver tom søknad for denne søknaden")
        hendelse.behov(Aldersbehov, "Behøver tom søknad for denne søknaden")

        assertThrows<IllegalArgumentException> { behovMediator.håndter(hendelse) }
    }

    private fun assertStandardBehovFelter(json: JsonNode) {
        assertEquals("behov", json["@event_name"].asText())
        assertTrue(json.hasNonNull("@id"))
        assertDoesNotThrow { UUID.fromString(json["@id"].asText()) }
        assertTrue(json.hasNonNull("@opprettet"))
        assertDoesNotThrow { LocalDateTime.parse(json["@opprettet"].asText()) }
    }

    private class Testkontekst(
        private val melding: String
    ) : Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(melding, mapOf(melding to melding))
    }

    private class TestHendelse(
        val logg: Aktivitetslogg
    ) : Hendelse(testIdent, logg), Aktivitetskontekst {
        init {
            logg.kontekst(this)
        }

        override fun toSpesifikkKontekst() = SpesifikkKontekst("TestHendelse")
        override fun kontekst(kontekst: Aktivitetskontekst) {
            logg.kontekst(kontekst)
        }
    }

    private class TestPersonKontekst(private val ident: String) : Aktivitetskontekst {
        override fun toSpesifikkKontekst() =
            SpesifikkKontekst(kontekstType = Person.kontekstType, mapOf("ident" to ident))
    }
}
