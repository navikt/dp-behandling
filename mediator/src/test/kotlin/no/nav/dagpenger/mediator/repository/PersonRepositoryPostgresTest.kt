package no.nav.dagpenger.mediator.repository

import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.dagpenger.mediator.Metrikk.hentPersonTimer
import no.nav.dagpenger.mediator.TestOpplysningstyper.heltall
import no.nav.dagpenger.mediator.TestOpplysningstyper.opplysningerRepository
import no.nav.dagpenger.mediator.db.DatabaseSession
import no.nav.dagpenger.mediator.db.withMigratedDb
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.modell.Person
import no.nav.dagpenger.modell.hendelser.StartHendelse
import no.nav.dagpenger.modell.hendelser.hendelseTypeOpplysningstype
import no.nav.dagpenger.modell.somKjede
import no.nav.dagpenger.opplysning.Faktum
import no.nav.dagpenger.opplysning.Prosessregister
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
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
            val hendelse = TestBehandlinger.lagTestHendelse(fnr, opplysninger = listOf(opplysning))
            lagreMelding(hendelse)
            val behandling = hendelse.opprettTestBehandling()
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
            lagreMelding(hendelse)
            val behandling = hendelse.opprettTestBehandling()
            val person = Person(ident, listOf(behandling.somKjede()))

            personRepositoryPostgres.lagre(person)
            personRepositoryPostgres.lagre(person)
        }

    @Test
    fun `hent finner person via alias-ident etter merge`() =
        personTest {
            val vinner = Ident("12345678901")
            val taper = Ident("10987654321")

            personRepositoryPostgres.lagre(Person(vinner))
            personRepositoryPostgres.lagre(Person(taper))

            personRepositoryPostgres.merge(winner = vinner, loser = taper)

            val funnetViaVinner = personRepositoryPostgres.hent(vinner)
            val funnetViaTaper = personRepositoryPostgres.hent(taper)

            // Begge peker på samme kanoniske ident (vinner) etter merge
            funnetViaVinner?.ident?.identifikator() shouldBe vinner.identifikator()
            funnetViaTaper?.ident?.identifikator() shouldBe vinner.identifikator()
            funnetViaVinner?.ident?.alleIdentifikatorer()!! shouldContain taper.identifikator()
        }

    @Test
    fun `harIdent returnerer true for både vinner og taper etter merge`() =
        personTest {
            val vinner = Ident("12345678901")
            val taper = Ident("10987654321")

            personRepositoryPostgres.lagre(Person(vinner))
            personRepositoryPostgres.lagre(Person(taper))
            personRepositoryPostgres.merge(winner = vinner, loser = taper)

            personRepositoryPostgres.harIdent(vinner) shouldBe true
            personRepositoryPostgres.harIdent(taper) shouldBe true
        }

    @Test
    fun `merge er idempotent`() =
        personTest {
            val vinner = Ident("12345678901")
            val taper = Ident("10987654321")

            personRepositoryPostgres.lagre(Person(vinner))
            personRepositoryPostgres.lagre(Person(taper))
            personRepositoryPostgres.merge(winner = vinner, loser = taper)
            personRepositoryPostgres.merge(winner = vinner, loser = taper)

            personRepositoryPostgres.hent(taper)?.ident?.identifikator() shouldBe vinner.identifikator()
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
                        opplysningerRepository(dbSession, listOf(hendelseTypeOpplysningstype)),
                        mockk(relaxed = true),
                        kildeRepository,
                        prosessregister,
                    ),
                )
            block(Persontest(fnr, personRepositoryPostgres, dbSession))
        }
    }

    // Sørger for at meldingsreferanseId til testhendelsen finnes i melding-tabellen, slik at Systemkilde
    // som opprettBehandling() legger til (via hendelseTypeOpplysningstype) kan lagres uten å bryte FK-constraint.
    private fun Persontest.lagreMelding(hendelse: StartHendelse) {
        dbSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO melding
                        (ident, melding_id, melding_type, data, lest_dato)
                    VALUES
                        (:ident, :melding_id, :melding_type, :data, NOW())
                    ON CONFLICT DO NOTHING
                    """.trimIndent(),
                    mapOf(
                        "ident" to hendelse.ident,
                        "melding_id" to hendelse.meldingsreferanseId,
                        "melding_type" to "Test",
                        "data" to
                            PGobject().apply {
                                type = "json"
                                value = "{}"
                            },
                    ),
                ).asUpdate,
            )
        }
    }

    private data class Persontest(
        val fnr: String,
        val personRepositoryPostgres: PersonRepositoryPostgres,
        val dbSession: DatabaseSession,
    ) {
        val ident = Ident(fnr)
    }
}
