package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.behandling.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.modell.Behandling
import no.nav.dagpenger.behandling.modell.Ident
import no.nav.dagpenger.behandling.modell.Person
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Heltall
import no.nav.dagpenger.opplysning.Opplysningstype
import no.nav.dagpenger.regel.hendelse.SøknadInnsendtHendelse
import no.nav.dagpenger.regel.hendelse.Søknadstype
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class PersonRepositoryPostgresTest {
    private val fnr = "12345678901"
    private val søknadId = UUIDv7.ny()
    private val personRepositoryPostgres
        get() =
            PersonRepositoryPostgres(
                BehandlingRepositoryPostgres(
                    opplysningerRepository(),
                    mockk(relaxed = true),
                ),
            )
    private val søknadInnsendtHendelse =
        SøknadInnsendtHendelse(
            meldingsreferanseId = søknadId,
            ident = fnr,
            søknadId = søknadId,
            gjelderDato = LocalDate.now(),
            fagsakId = 1,
            opprettet = LocalDateTime.now(),
            Søknadstype.NySøknad,
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
            actualPerson?.harRettighet(LocalDate.now()) shouldBe false

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
            val opplysning = Faktum(Opplysningstype.heltall(Opplysningstype.Id(UUIDv7.ny(), Heltall), "Heltall"), 5)
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
                    .flatMap { behandling -> behandling.opplysninger().somListe() } shouldContain opplysning
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
