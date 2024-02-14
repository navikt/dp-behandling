package no.nav.dagpenger.vedtak

import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.vedtak.db.InMemoryMeldingRepository
import no.nav.dagpenger.vedtak.db.InMemoryPersonRepository
import no.nav.dagpenger.vedtak.mediator.BehovMediator
import no.nav.dagpenger.vedtak.mediator.HendelseMediator
import no.nav.dagpenger.vedtak.mediator.PersonMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PersonMediatorTest {
    private val testRapid = TestRapid()
    private val ident = "11109233444"

    private val personRepository = InMemoryPersonRepository()

    init {
        HendelseMediator(
            rapidsConnection = testRapid,
            personMediator =
                PersonMediator(
                    personRepository = personRepository,
                    aktivitetsloggMediator = mockk(relaxed = true),
                    behovMediator = BehovMediator(testRapid),
                ),
            hendelseRepository = InMemoryMeldingRepository(),
        )
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
        personRepository.reset()
    }

    @Test
    fun `e2e av søknad innsendt`() {
        testRapid.sendTestMessage(søknadInnsendtMessage(ident))

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@event_name").asText() shouldBe "behov"
            field(0, "@behov").map { it.asText() }.shouldContainOnly(
                "Fødselsdato",
                "Søknadstidspunkt",
            )
        }
        testRapid.sendTestMessage(opplysningSvar1Message(ident))

        with(testRapid.inspektør) {
            size shouldBe 2
            field(1, "@behov").map { it.asText() }.shouldContainOnly(
                "InntektSiste12Mnd",
                "InntektSiste3År",
            )
            with(field(1, "InntektSiste12Mnd")) {
                this.size() shouldBe 1
                get("Virkningsdato").asText() shouldBe LocalDate.now().toString()
            }
            with(field(1, "InntektSiste3År")) {
                this.size() shouldBe 1
                get("Virkningsdato").asText() shouldBe LocalDate.now().toString()
            }
        }

        testRapid.sendTestMessage(opplysningSvar2Message(ident))
    }

    private fun søknadInnsendtMessage(ident: String) =
        JsonMessage.newMessage(
            "innsending_ferdigstilt",
            mapOf(
                "type" to "NySøknad",
                "fødselsnummer" to ident,
                "søknadsData" to
                    mapOf(
                        "søknad_uuid" to "e2e9e3e0-8e3e-4e3e-8e3e-0e3e8e3e9e3e",
                    ),
            ),
        ).toJson()

    private fun opplysningSvar1Message(ident: String) =
        JsonMessage.newMessage(
            "behov",
            mapOf(
                "ident" to ident,
                "behandlingId" to "e2e9e3e0-8e3e-4e3e-8e3e-0e3e8e3e9e3e",
                "@løsning" to
                    mapOf(
                        "Fødselsdato" to LocalDate.of(1990, 1, 1),
                        "Søknadstidspunkt" to LocalDate.now(),
                    ),
            ),
        ).toJson()

    private fun opplysningSvar2Message(ident: String) =
        JsonMessage.newMessage(
            "behov",
            mapOf(
                "ident" to ident,
                "behandlingId" to "e2e9e3e0-8e3e-4e3e-8e3e-0e3e8e3e9e3e",
                "@løsning" to
                    mapOf(
                        "InntektSiste12Mnd" to 1234,
                        "InntektSiste3År" to 1234,
                    ),
            ),
        ).toJson()
}
