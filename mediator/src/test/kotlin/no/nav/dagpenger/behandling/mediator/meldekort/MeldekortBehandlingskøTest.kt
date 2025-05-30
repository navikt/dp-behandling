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
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class MeldekortBehandlingskøTest {
    private val rapid = TestRapid()
    private val personRepository = mockk<PersonRepository>()
    private val meldekortRepository = MeldekortRepositoryPostgres()

    private val ident = "12345678901"

    @Test
    fun `tester kø`() {
        withMigratedDb {
            val meldekort = MeldekortBehandlingskø(personRepository, meldekortRepository, rapid)
            lagPerson(1.januar(2024))

            val person = meldekortRepository.generatorFor(ident, 1.januar(2024))
            person.lagMeldekort(2)

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

    private fun lagPerson(innvilget: LocalDate) {
        every {
            personRepository.rettighetstatusFor(ident.tilPersonIdentfikator())
        } returns
            TemporalCollection<Rettighetstatus>().apply {
                put(
                    innvilget,
                    Rettighetstatus(virkningsdato = innvilget, utfall = true, behandlingId = UUID.randomUUID()),
                )
            }
    }
}
