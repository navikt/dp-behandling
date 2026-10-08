package no.nav.dagpenger.mediator.db

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

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
            // Migrer kun til rett før periodiseringen, slik at vi kan sette inn rader i det gamle
            // brytepunkt-skjemaet (gjelder_fra/virkningsdato, uten regelverk/til_og_med/opplysning_id).
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
                            mapOf("ident" to ident),
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

            val nilUuid = UUID.fromString("00000000-0000-0000-0000-000000000000")
            rader shouldBe
                listOf(
                    Rad(LocalDate.of(2024, 1, 1), "2024-05-31", true, "Dagpenger", nilUuid),
                    Rad(LocalDate.of(2024, 6, 1), "2024-08-31", false, "Dagpenger", nilUuid),
                    Rad(LocalDate.of(2024, 9, 1), "infinity", true, "Dagpenger", nilUuid),
                )
        }
    }
}
