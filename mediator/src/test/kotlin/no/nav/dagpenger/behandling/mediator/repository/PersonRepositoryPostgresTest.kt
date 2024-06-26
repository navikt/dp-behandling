package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.behandling.modell.UUIDv7
import no.nav.dagpenger.behandling.modell.hendelser.SøknadInnsendtHendelse
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Opplysningstype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonRepositoryPostgresTest {
    private val fnr = "12345678901"
    private val søknadId = UUIDv7.ny()
    private val personRepositoryPostgres
        get() =
            PersonRepositoryPostgres(
                BehandlingRepositoryPostgres(
                    OpplysningerRepositoryPostgres(),
                    mockk(relaxed = true),
                ),
            )
    private val søknadInnsendtHendelse =
        SøknadInnsendtHendelse(
            søknadId = søknadId,
            ident = fnr,
            meldingsreferanseId = søknadId,
            gjelderDato = LocalDate.now(),
            fagsakId = 1,
        )

    @Test
    fun `hent returnerer person når personen finnes i databasen`() =
        withMigratedDb {
            val ident = Ident(fnr)
            val expectedPerson =
                Person(ident, emptyList()).also {
                    personRepositoryPostgres.lagre(it)
                }

            val actualPerson = personRepositoryPostgres.hent(ident)

            assertEquals(expectedPerson.ident, actualPerson?.ident)
        }

    @Test
    fun `hent returnerer null når personen ikke finnes i databasen`() =
        withMigratedDb {
            val ident = Ident(fnr)

            val actualPerson = personRepositoryPostgres.hent(ident)

            assertNull(actualPerson)
        }

    @Test
    fun `lagre setter inn person og deres behandlinger i databasen`() =
        withMigratedDb {
            val ident = Ident(fnr)
            val opplysning = Faktum(Opplysningstype.somHeltall("Heltall"), 5)
            val behandling = Behandling(søknadInnsendtHendelse, listOf(opplysning))
            val person = Person(ident, listOf(behandling))

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
                    .flatMap { behandling -> behandling.opplysninger().finnAlle() } shouldContain opplysning
            }
        }

    @Test
    fun `lagre setter ikke inn person i databasen når personen allerede finnes`() =
        withMigratedDb {
            val ident = Ident(fnr)
            val behandling = Behandling(søknadInnsendtHendelse, emptyList())
            val person = Person(ident, listOf(behandling))

            personRepositoryPostgres.lagre(person)
            personRepositoryPostgres.lagre(person)
        }
}
