package no.nav.dagpenger.behandling.mediator.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behandling.mediator.repository.BehandlingRepository
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Behandlingkjede
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Gyldighetsperiode
import no.nav.dagpenger.opplysning.LesbarOpplysninger
import no.nav.dagpenger.regel.beregning.Beregning
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BehovsløserForbruksdagerMottakTest {
    private val repository = mockk<BehandlingRepository>()
    private val rapid =
        TestRapid().also {
            BehovsløserForbruksdagerMottak(it, repository)
        }

    @BeforeEach
    fun reset() {
        rapid.reset()
    }

    @Test
    fun `teller forbruksdager innenfor opptjeningsåret`() {
        val ident = "12345678901"
        val opptjeningsår = 2025

        // Lag opplysninger om forbruk: 5 dager i januar 2025
        val forbruksOpplysninger =
            listOf(
                // 5 dager
                Faktum(
                    Beregning.forbruk,
                    true,
                    Gyldighetsperiode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 10)),
                ),
                // 1 dag
                Faktum(
                    Beregning.forbruk,
                    true,
                    Gyldighetsperiode(LocalDate.of(2025, 8, 6), LocalDate.of(2025, 8, 6)),
                ),
                // 3 dager i februar 2025
                Faktum(
                    Beregning.forbruk,
                    true,
                    Gyldighetsperiode(LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 5)),
                ),
                // Dag i 2024 - skal ikke telles
                Faktum(
                    Beregning.forbruk,
                    true,
                    Gyldighetsperiode(LocalDate.of(2024, 10, 30), LocalDate.of(2024, 10, 31)),
                ),
                // Dager i 2024 - skal ikke telles, men den ene i 2025 skal være med
                Faktum(
                    Beregning.forbruk,
                    true,
                    Gyldighetsperiode(LocalDate.of(2024, 12, 30), LocalDate.of(2025, 1, 1)),
                ),
                // Forbruk = false - skal ikke telles
                Faktum(
                    Beregning.forbruk,
                    false,
                    Gyldighetsperiode(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 7)),
                ),
            )

        val opplysninger = mockk<LesbarOpplysninger>()
        every { opplysninger.finnAlle(Beregning.forbruk) } returns forbruksOpplysninger

        val behandling = mockk<Behandling>()
        every { behandling.opplysninger() } returns opplysninger

        val kjede = mockk<Behandlingkjede>()
        every { kjede.nesteSomKanBaseresPå } returns behandling

        every { repository.hentBehandlinger(Ident(ident)) } returns listOf(kjede)

        finnAntallDagerForbruk(listOf(behandling), opptjeningsår) shouldBe 10L

        rapid.sendTestMessage(lagBehov(ident, opptjeningsår))

        val løsning = rapid.inspektør.message(0)
        løsning["@løsning"]["AntallDagerForbrukt"]["verdi"].asLong() shouldBe 10L // 5 + 1 + 3 + 1
        løsning["@løsning"]["AntallDagerForbrukt"]["gyldigFraOgMed"].asText() shouldBe "2025-01-01"
    }

    @Test
    fun `returnerer 0 når det ikke finnes behandlinger`() {
        val ident = "12345678901"
        val opptjeningsår = 2025

        every { repository.hentBehandlinger(Ident(ident)) } returns emptyList()

        rapid.sendTestMessage(lagBehov(ident, opptjeningsår))

        val løsning = rapid.inspektør.message(0)
        løsning["@løsning"]["AntallDagerForbrukt"]["verdi"].asLong() shouldBe 0L
    }

    @Test
    fun `teller forbruksdager fra flere behandlingskjeder`() {
        val ident = "12345678901"
        val opptjeningsår = 2025

        val opplysninger1 = mockk<LesbarOpplysninger>()
        every { opplysninger1.finnAlle(Beregning.forbruk) } returns
            listOf(
                Faktum(
                    Beregning.forbruk,
                    true,
                    Gyldighetsperiode(LocalDate.of(2025, 1, 6), LocalDate.of(2025, 1, 10)),
                ),
            )

        val opplysninger2 = mockk<LesbarOpplysninger>()
        every { opplysninger2.finnAlle(Beregning.forbruk) } returns
            listOf(
                Faktum(
                    Beregning.forbruk,
                    true,
                    Gyldighetsperiode(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 5)),
                ),
            )

        val behandling1 = mockk<Behandling>()
        every { behandling1.opplysninger() } returns opplysninger1

        val behandling2 = mockk<Behandling>()
        every { behandling2.opplysninger() } returns opplysninger2

        val kjede1 = mockk<Behandlingkjede>()
        every { kjede1.nesteSomKanBaseresPå } returns behandling1

        val kjede2 = mockk<Behandlingkjede>()
        every { kjede2.nesteSomKanBaseresPå } returns behandling2

        every { repository.hentBehandlinger(Ident(ident)) } returns listOf(kjede1, kjede2)

        finnAntallDagerForbruk(listOf(behandling1, behandling2), opptjeningsår) shouldBe 8L

        rapid.sendTestMessage(lagBehov(ident, opptjeningsår))

        val løsning = rapid.inspektør.message(0)
        løsning["@løsning"]["AntallDagerForbrukt"]["verdi"].asLong() shouldBe 8L // 5 + 3
    }

    private fun lagBehov(
        ident: String,
        opptjeningsår: Int,
    ) = JsonMessage
        .newMessage(
            "behov",
            mapOf(
                "@behov" to listOf("AntallDagerForbrukt"),
                "@behovId" to UUID.randomUUID().toString(),
                "ident" to ident,
                "AntallDagerForbrukt" to
                    mapOf(
                        "OpptjeningsårFerietillegg" to opptjeningsår,
                    ),
            ),
        ).toJson()
}
