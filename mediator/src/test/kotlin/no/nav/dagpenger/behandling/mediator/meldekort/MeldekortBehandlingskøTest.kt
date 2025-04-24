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
import no.nav.dagpenger.behandling.modell.hendelser.Meldekort
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKilde
import no.nav.dagpenger.opplysning.TemporalCollection
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MeldekortBehandlingskøTest {
    private val rapid = TestRapid()
    private val personRepository = mockk<PersonRepository>()
    private val meldekortRepository = MeldekortRepositoryPostgres()
    private val meldeperiode = Meldeperiode(LocalDate.now())

    private var eksternMeldekortId: Long = 1
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

    private fun nyttMeldekort(): Meldekort {
        val meldingsreferanseId = UUID.randomUUID()
        val periode = meldeperiode.neste()

        val meldekort =
            Meldekort(
                id = UUID.randomUUID(),
                meldingsreferanseId = meldingsreferanseId,
                ident = ident,
                eksternMeldekortId = eksternMeldekortId++,
                fom = periode.start,
                tom = periode.endInclusive,
                kilde = MeldekortKilde("Bruker", ident),
                dager = listOf(),
                innsendtTidspunkt = LocalDateTime.now(),
                korrigeringAv = null,
            )
        return meldekort
    }

    private class Meldeperiode(
        start: LocalDate,
    ) {
        private val aktiv = start

        fun neste(): ClosedRange<LocalDate> = aktiv..aktiv.plusDays(14)
    }
}
