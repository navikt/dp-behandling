package no.nav.dagpenger.behandling.mediator.meldekort

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.januar
import no.nav.dagpenger.behandling.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.behandling.mediator.repository.Meldekortgenerator.Companion.generatorFor
import no.nav.dagpenger.behandling.mediator.repository.PersonRepository
import no.nav.dagpenger.behandling.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.behandling.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import javax.sql.DataSource

class MeldekortBehandlingskøTest {
    private val ident = "12345678901"

    @Test
    fun `tester kø`() {
        e2eTest {
            val meldekort = MeldekortBehandlingskø(personRepository, meldekortRepository, rapid, dataSource)
            lagPerson(1.januar(2024))

            val person = meldekortRepository.generatorFor(ident, 1.januar(2024))
            person.lagMeldekort(dataSource, 2)

            // Første meldekort behandles
            meldekort.sendMeldekortTilBehandling()
            rapid.inspektør.size shouldBe 1

            // Første meldekort behandles fortsatt
            meldekort.sendMeldekortTilBehandling()
            rapid.inspektør.size shouldBe 1

            // Første meldekort er ferdig behandlet
            person.markerFerdig(1)

            // Andre meldekort startes
            meldekort.sendMeldekortTilBehandling()
            rapid.inspektør.size shouldBe 2
        }
    }

    private fun E2ETextContext.lagPerson(innvilget: LocalDate) {
        every {
            personRepository.rettighetstatusFor(ident.tilPersonIdentfikator())
        } returns
            TemporalCollection<Rettighetstatus>().apply {
                val behandlingId = UUIDv7.ny()
                put(
                    innvilget,
                    Rettighetstatus(
                        virkningsdato = innvilget,
                        utfall = true,
                        behandlingId = behandlingId,
                        behandlingskjedeId = behandlingId,
                    ),
                )
            }
    }
}

private data class E2ETextContext(
    val personRepository: PersonRepository,
    val meldekortRepository: MeldekortRepositoryPostgres,
    val rapid: TestRapid,
    val dataSource: DataSource,
)

private fun e2eTest(block: E2ETextContext.() -> Unit) {
    withMigratedDb {
        val rapid = TestRapid()
        val personRepository = mockk<PersonRepository>()
        val meldekortRepository = MeldekortRepositoryPostgres(dataSource)

        val testContext = E2ETextContext(personRepository, meldekortRepository, rapid, dataSource)
        block(testContext)
    }
}
