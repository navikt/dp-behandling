package no.nav.dagpenger.mediator.db

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.dagpenger.mediator.repository.PersonRepositoryPostgres
import no.nav.dagpenger.mediator.repository.TestBehandlinger
import no.nav.dagpenger.modell.Arbeidssteg
import no.nav.dagpenger.modell.Behandling
import no.nav.dagpenger.modell.BehandlingObservatør.BehandlingFerdig
import no.nav.dagpenger.modell.Ident
import no.nav.dagpenger.modell.Person
import no.nav.dagpenger.opplysning.Avgjørelse
import no.nav.dagpenger.opplysning.Opplysninger
import no.nav.dagpenger.opplysning.RegelverkIdent
import no.nav.dagpenger.opplysning.Rettighetsperiode
import no.nav.dagpenger.uuid.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val nilUuid: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
private val dagpenger = RegelverkIdent("Dagpenger")

/**
 * Verifiserer at V103 (rettighetstatus fra brytepunkter til perioder) faktisk bakfyller eksisterende data
 * riktig, i stedet for å basere seg på en TRUNCATE. Se V103__RETTIGHETSTATUS_PERIODER.sql for bakgrunn:
 * uten backfill ville MeldekortBehandlingskø aldri funnet overlapp mot en tom tabell, og dermed aldri
 * sendt løpende saker til behandling igjen.
 */
class RettighetstatusBackfillTest {
    @Test
    fun `V103 utleder regelverk, til_og_med og opplysning_id fra gamle brytepunkt-rader`() {
        withIsolatedDb {
            val kjede = settOppBakfyltKjede()

            data class Rad(
                val fraOgMed: LocalDate,
                val tilOgMed: String,
                val harRettighet: Boolean,
                val regelverk: String,
                val opplysningId: UUID?,
            )

            val rader =
                dbSession.session { session ->
                    session.run(
                        queryOf(
                            //language=PostgreSQL
                            """
                            SELECT fra_og_med, til_og_med::text AS til_og_med, har_rettighet, regelverk, opplysning_id
                            FROM rettighetstatus
                            WHERE ident = :ident
                            ORDER BY fra_og_med
                            """.trimIndent(),
                            mapOf("ident" to kjede.ident),
                        ).map { row ->
                            Rad(
                                fraOgMed = row.localDate("fra_og_med"),
                                tilOgMed = row.string("til_og_med"),
                                harRettighet = row.boolean("har_rettighet"),
                                regelverk = row.string("regelverk"),
                                opplysningId = row.uuidOrNull("opplysning_id"),
                            )
                        }.asList,
                    )
                }

            rader shouldBe
                listOf(
                    Rad(LocalDate.of(2024, 1, 1), "2024-05-31", true, "Dagpenger", nilUuid),
                    Rad(LocalDate.of(2024, 6, 1), "2024-08-31", false, "Dagpenger", nilUuid),
                    Rad(LocalDate.of(2024, 9, 1), "infinity", true, "Dagpenger", nilUuid),
                )
        }
    }

    @Test
    fun `Rettighetstidslinje#oppdater() nullstiller opphav til ny behandling når en bakfylt kjede berøres på nytt`() {
        withIsolatedDb {
            val kjede = settOppBakfyltKjede()
            val personRepository = PersonRepositoryPostgres(dbSession, mockk(relaxed = true))

            // Sanity: periodene fra backfillen har nil-UUID som opplysningId, akkurat slik de ville ligget
            // i databasen etter en reell V103-migrering.
            val bakfyltTidslinje = personRepository.rettighetstatusFor(Ident(kjede.ident))
            bakfyltTidslinje.perioder(dagpenger).map { it.opplysningId } shouldBe listOf(nilUuid, nilUuid, nilUuid)

            val person = Person(Ident(kjede.ident), emptyList(), bakfyltTidslinje)
            val nyBehandlingId = UUIDv7.ny()
            dbSession.session { session ->
                session.run(
                    queryOf(
                        "INSERT INTO behandling (behandling_id, tilstand) VALUES (:id, 'Ferdig')",
                        mapOf("id" to nyBehandlingId),
                    ).asExecute,
                )
            }

            val nyePerioder =
                listOf(
                    Rettighetsperiode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 31), harRett = true, endret = false),
                    Rettighetsperiode(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 8, 31), harRett = false, endret = false),
                    Rettighetsperiode(LocalDate.of(2024, 9, 1), LocalDate.MAX, harRett = true, endret = true),
                )

            // Simulerer at kjeden blir "ferdig" på nytt etter migrering - det eneste produksjonskoden faktisk
            // gjør med en rettighetstidslinje (se Person.ferdig).
            person.ferdig(nyttVedtak(kjede.ident, nyBehandlingId, kjede.behandlingskjedeId, nyePerioder))

            val oppdaterteStatuser = person.rettighetstidslinje().perioder(dagpenger)
            oppdaterteStatuser shouldHaveSize 3
            // Siden ingen ekte opplysningId (alltid UUIDv7) noensinne kan matche nil-UUID-plassholderen,
            // blir ALLE periodene i kjeden attribuert til den nye behandlingen - en forventet, engangs
            // "opphav-nullstilling" for kjeder som bakfylles og deretter berøres på nytt.
            oppdaterteStatuser.map { it.behandlingId }.toSet() shouldBe setOf(nyBehandlingId)
            oppdaterteStatuser.map { it.behandlingskjedeId }.toSet() shouldBe setOf(kjede.behandlingskjedeId)
            oppdaterteStatuser.none { it.opplysningId == nilUuid } shouldBe true

            // Rundtur mot databasen: lagring og ny innlesing skal gi samme resultat.
            personRepository.lagre(person)
            val fraDbEtterOppdatering = personRepository.rettighetstatusFor(Ident(kjede.ident))
            fraDbEtterOppdatering.perioder(dagpenger) shouldBe oppdaterteStatuser
        }
    }

    private fun nyttVedtak(
        ident: String,
        behandlingId: UUID,
        behandlingskjedeId: UUID,
        perioder: List<Rettighetsperiode>,
    ): BehandlingFerdig =
        BehandlingFerdig(
            Behandling.Resultat(
                behandlingId = behandlingId,
                basertPåBehandling = null,
                behandlingskjedeId = behandlingskjedeId,
                regelverk = dagpenger,
                rettighetsperioder = perioder,
                avgjørelse = Avgjørelse.Innvilgelse,
                virkningsdato = perioder.first().fraOgMed,
                behandlingAv = TestBehandlinger.lagTestHendelse(ident),
                opplysninger = Opplysninger(),
                automatiskBehandlet = true,
                godkjentAv = Arbeidssteg(Arbeidssteg.Oppgave.Godkjent),
                besluttetAv = Arbeidssteg(Arbeidssteg.Oppgave.Besluttet),
                opprettet = LocalDateTime.now(),
                sistEndret = LocalDateTime.now(),
            ),
        )

    private data class BakfyltKjede(
        val ident: String,
        val behandlingId1: UUID,
        val behandlingId2: UUID,
        val behandlingId3: UUID,
        val behandlingskjedeId: UUID,
    )

    /**
     * Migrerer kun til rett før periodiseringen, setter inn rader i det gamle brytepunkt-skjemaet
     * (gjelder_fra/virkningsdato, uten regelverk/til_og_med/opplysning_id), og kjører deretter V103
     * (og resten av migreringene) slik at backfillen faktisk utføres.
     */
    private fun DBTestContext.settOppBakfyltKjede(): BakfyltKjede {
        runMigrationTo("102")

        val ident = "12345678901"
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        val behandlingId3 = UUID.randomUUID()
        val behandlingskjedeId = UUID.randomUUID()

        dbSession.session { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf("INSERT INTO person (ident) VALUES (:ident)", mapOf("ident" to ident)).asExecute,
                )

                // rettighetstatus har FK mot behandling for både behandling_id og behandlingskjede_id -
                // minimum gyldig rad der er (behandling_id, tilstand).
                listOf(behandlingId1, behandlingId2, behandlingId3, behandlingskjedeId).forEach { id ->
                    tx.run(
                        queryOf(
                            "INSERT INTO behandling (behandling_id, tilstand) VALUES (:id, 'Ferdig')",
                            mapOf("id" to id),
                        ).asExecute,
                    )
                }

                tx.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        INSERT INTO rettighetstatus
                            (ident, gjelder_fra, virkningsdato, har_rettighet, behandling_id, behandlingskjede_id)
                        VALUES
                            (:ident, :dato1, :dato1, true, :b1, :kjede),
                            (:ident, :dato2, :dato2, false, :b2, :kjede),
                            (:ident, :dato3, :dato3, true, :b3, :kjede)
                        """.trimIndent(),
                        mapOf(
                            "ident" to ident,
                            "dato1" to LocalDate.of(2024, 1, 1),
                            "dato2" to LocalDate.of(2024, 6, 1),
                            "dato3" to LocalDate.of(2024, 9, 1),
                            "b1" to behandlingId1,
                            "b2" to behandlingId2,
                            "b3" to behandlingId3,
                            "kjede" to behandlingskjedeId,
                        ),
                    ).asExecute,
                )
            }
        }

        // Kjører V103 og resten av migreringene.
        runMigration()

        return BakfyltKjede(ident, behandlingId1, behandlingId2, behandlingId3, behandlingskjedeId)
    }
}
