package no.nav.dagpenger.mediator.meldekort

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mediator.db.withMigratedDb
import no.nav.dagpenger.mediator.januar
import no.nav.dagpenger.mediator.repository.MeldekortRepositoryPostgres
import no.nav.dagpenger.mediator.repository.Meldekortgenerator.Companion.generatorFor
import no.nav.dagpenger.mediator.repository.PersonRepository
import no.nav.dagpenger.modell.Ident.Companion.tilPersonIdentfikator
import no.nav.dagpenger.modell.Rettighetstatus
import no.nav.dagpenger.opplysning.TemporalCollection
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortBehandlingskøTest {
    private val rapid = TestRapid()
    private val personRepository = mockk<PersonRepository>()

    private val ident = "12345678901"

    @Test
    fun `tester kø`() {
        withMigratedDb {
            val meldekortRepository = MeldekortRepositoryPostgres(dbSession)
            // Bruker en eksplisitt virkedag (mandag) for å unngå flaky tester på helger/helligdager
            val kjøringsdato = LocalDate.of(2024, 1, 29)
            val meldekort = MeldekortBehandlingskø(dbSession, personRepository, meldekortRepository, rapid)
            lagPerson(1.januar(2024))

            val person = meldekortRepository.generatorFor(dbSession, ident, 1.januar(2024))
            person.lagMeldekort(2)

            // Første meldekort behandles
            meldekort.sendMeldekortTilBehandling(kjøringsdato)
            rapid.inspektør.size shouldBe 1

            // Første meldekort behandles fortsatt
            meldekort.sendMeldekortTilBehandling(kjøringsdato)
            rapid.inspektør.size shouldBe 1

            // Første meldekort er ferdig behandlet
            person.markerFerdig(1)

            // Andre meldekort startes
            meldekort.sendMeldekortTilBehandling(kjøringsdato)
            rapid.inspektør.size shouldBe 2
        }
    }

    private fun lagPerson(innvilget: LocalDate) {
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
