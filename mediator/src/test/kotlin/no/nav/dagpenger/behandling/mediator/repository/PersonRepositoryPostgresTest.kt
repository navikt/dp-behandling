package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.behandling.TestOpplysningstyper.heltall
import no.nav.dagpenger.behandling.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.behandling.db.withMigratedDb
import no.nav.dagpenger.behandling.mediator.Metrikk.hentPersonTimer
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.somKjede
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Prosessregister
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonRepositoryPostgresTest {
    @Test
    fun `hent returnerer person når personen finnes i databasen`() =
        personTest {
            val expectedPerson =
                Person(ident, emptyList()).also {
                    personRepositoryPostgres.lagre(it)
                }

            val actualPerson = personRepositoryPostgres.hent(ident)
            actualPerson?.harRettighet(LocalDate.now()) shouldBe false

            assertEquals(expectedPerson.ident, actualPerson?.ident)

            // Sjekk at det er brukt et sted mellom 0 og 0.5 sekunder.
            hentPersonTimer
                .collect()
                .dataPoints
                .shouldForAll {
                    it.sum shouldBeGreaterThan 0.0
                    it.exemplars.size() shouldBe 0
                }
        }

    @Test
    fun `hent returnerer null når personen ikke finnes i databasen`() =
        personTest {
            val actualPerson = personRepositoryPostgres.hent(ident)

            assertNull(actualPerson)
        }

    @Test
    fun `lagre setter inn person og deres behandlinger i databasen`() =
        personTest {
            val opplysning = Faktum(heltall, 5)
            val hendelse = TestBehandlinger.lagTestHendelse(fnr)
            val behandling = Behandling(hendelse, listOf(opplysning))
            val person = Person(ident, listOf(behandling.somKjede()))

            personRepositoryPostgres.lagre(person)

            val fraDb = personRepositoryPostgres.hent(ident)
            fraDb?.let {
                it.ident shouldBe person.ident
                it.behandlinger() shouldContain behandling
                it.behandlinger().first().behandlingId shouldBe behandling.behandlingId
                it
                    .behandlinger()
                    .first()
                    .opplysninger()
                    .id shouldBe behandling.opplysninger().id
                it
                    .behandlinger()
                    .flatMap { behandling -> behandling.opplysninger().somListe() } shouldContain opplysning
            }
        }

    @Test
    fun `lagre setter ikke inn person i databasen når personen allerede finnes`() =
        personTest {
            val hendelse = TestBehandlinger.lagTestHendelse(fnr)
            val behandling = Behandling(hendelse, emptyList())
            val person = Person(ident, listOf(behandling.somKjede()))

            personRepositoryPostgres.lagre(person)
            personRepositoryPostgres.lagre(person)
        }

    private fun personTest(block: Persontest.() -> Unit) {
        withMigratedDb {
            val fnr = "12345678901"
            val prosessregister =
                Prosessregister().also {
                    TestBehandlinger.registrerTestProsesser(it)
                }
            val kildeRepository = KildeRepository(dbSession)
            val personRepositoryPostgres =
                PersonRepositoryPostgres(
                    dbSession,
                    BehandlingRepositoryPostgres(
                        dbSession,
                        opplysningerRepository(dbSession),
                        mockk(relaxed = true),
                        kildeRepository,
                        prosessregister,
                    ),
                )
            block(Persontest(fnr, personRepositoryPostgres))
        }
    }

    private data class Persontest(
        val fnr: String,
        val personRepositoryPostgres: PersonRepositoryPostgres,
    ) {
        val ident = Ident(fnr)
    }
}
