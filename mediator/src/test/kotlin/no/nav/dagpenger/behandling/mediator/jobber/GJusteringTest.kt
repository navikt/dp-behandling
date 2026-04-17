package no.nav.dagpenger.behandling.mediator.jobber

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepository
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.regel.OpplysningsTyper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class GJusteringTest {
    private val repository = mockk<BehandlingRepository>()
    private val rapid = TestRapid()
    private val gJustering = GJustering(repository)

    private val fraOgMed = LocalDate.of(2025, 5, 1)
    private val tilOgMed = LocalDate.of(2025, 5, 20)
    private val ident = "12345678901"

    @BeforeEach
    fun setup() {
        rapid.reset()
    }

    @Test
    fun `publiserer rekjør_behandling for åpen behandling`() {
        val behandlingId = UUID.randomUUID()
        val behandling = lagBehandlingMock(behandlingId, ident, Behandling.TilstandType.UnderBehandling)

        every { repository.finnBehandlingerForGJustering(fraOgMed, tilOgMed, any()) } answers {
            val block = arg<(Behandling) -> Unit>(2)
            block(behandling)
        }

        gJustering.startGjustering(fraOgMed, tilOgMed, rapid)

        rapid.inspektør.size shouldBe 1
        with(rapid.inspektør.message(0)) {
            this["@event_name"].asText() shouldBe "rekjør_behandling"
            this["ident"].asText() shouldBe ident
            this["behandlingId"].asText() shouldBe behandlingId.toString()
            this["oppfriskOpplysningIder"].first().asText() shouldBe
                OpplysningsTyper.GrunnbeløpForGrunnlagId.uuid.toString()
        }
    }

    @Test
    fun `publiserer omgjør_behandling for ferdig behandling`() {
        val behandling = lagBehandlingMock(UUID.randomUUID(), ident, Behandling.TilstandType.Ferdig)

        every { repository.finnBehandlingerForGJustering(fraOgMed, tilOgMed, any()) } answers {
            val block = arg<(Behandling) -> Unit>(2)
            block(behandling)
        }

        gJustering.startGjustering(fraOgMed, tilOgMed, rapid)

        rapid.inspektør.size shouldBe 1
        with(rapid.inspektør.message(0)) {
            this["@event_name"].asText() shouldBe "omgjør_behandling"
            this["ident"].asText() shouldBe ident
            this["gjelderDato"].asText() shouldBe fraOgMed.toString()
        }
    }

    @Test
    fun `håndterer mix av åpne og ferdige behandlinger`() {
        val åpenBehandlingId = UUID.randomUUID()
        val åpen = lagBehandlingMock(åpenBehandlingId, ident, Behandling.TilstandType.ForslagTilVedtak)
        val ferdig = lagBehandlingMock(UUID.randomUUID(), ident, Behandling.TilstandType.Ferdig)

        every { repository.finnBehandlingerForGJustering(fraOgMed, tilOgMed, any()) } answers {
            val block = arg<(Behandling) -> Unit>(2)
            block(åpen)
            block(ferdig)
        }

        gJustering.startGjustering(fraOgMed, tilOgMed, rapid)

        rapid.inspektør.size shouldBe 2
        val eventNavn = (0..<2).map { rapid.inspektør.message(it)["@event_name"].asText() }.toSet()
        eventNavn shouldBe setOf("rekjør_behandling", "omgjør_behandling")
    }

    @Test
    fun `publiserer ingen events når ingen kandidater finnes`() {
        every { repository.finnBehandlingerForGJustering(fraOgMed, tilOgMed, any()) } answers {}

        gJustering.startGjustering(fraOgMed, tilOgMed, rapid)

        rapid.inspektør.size shouldBe 0
    }

    private fun lagBehandlingMock(
        behandlingId: UUID,
        ident: String,
        tilstand: Behandling.TilstandType,
    ): Behandling {
        val hendelse = mockk<no.nav.dagpenger.behandling.modell.hendelser.StartHendelse>()
        every { hendelse.ident } returns ident
        val behandling = mockk<Behandling>()
        every { behandling.behandlingId } returns behandlingId
        every { behandling.behandler } returns hendelse
        every { behandling.tilstand() } returns Pair(tilstand, java.time.LocalDateTime.now())
        return behandling
    }
}
