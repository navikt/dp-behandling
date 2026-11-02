package no.nav.dagpenger.mediator.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.postgresql.util.PSQLException
import java.util.UUID

class PostgresMigrationTest {
    @Test
    fun `Migration scripts are applied successfully`() {
        withIsolatedDb {
            val migrations = runMigration()
            migrations shouldBeExactly 39
        }
    }

    @Test
    fun `krever kjede-ID for nye behandlinger etter migrering`() {
        withMigratedDb {
            val feil =
                shouldThrow<PSQLException> {
                    dbSession.session { session ->
                        session.run(
                            queryOf(
                                "INSERT INTO behandling (behandling_id, tilstand) VALUES (:id, 'UnderBehandling')",
                                mapOf("id" to UUID.randomUUID()),
                            ).asUpdate,
                        )
                    }
                }
            feil.sqlState shouldBe "23502"
            feil.serverErrorMessage?.column shouldBe "behandlingskjede_id"
        }
    }

    @Test
    fun `tilbakefyller roten for eksisterende behandlinger og grener uavhengig av UUID-rekkefolge`() {
        withIsolatedDb {
            Flyway
                .configure()
                .dataSource(hikariConfig.jdbcUrl, hikariConfig.username, hikariConfig.password)
                .target("105")
                .load()
                .migrate()

            val rot = UUID(0, 3)
            val barn = UUID(0, 1)
            val barnebarn = UUID(0, 2)
            val søsken = UUID(0, 4)
            val annenRot = UUID(0, 5)
            dbSession.session { session ->
                session.run(
                    queryOf(
                        """
                        INSERT INTO behandling (behandling_id, basert_på_behandling_id, tilstand, sist_endret_tilstand)
                        VALUES (:rot, NULL, 'Ferdig', NOW()),
                               (:barn, :rot, 'Ferdig', NOW()),
                               (:barnebarn, :barn, 'UnderBehandling', NOW()),
                               (:sosken, :rot, 'UnderBehandling', NOW()),
                               (:annenRot, NULL, 'UnderBehandling', NOW())
                        """.trimIndent(),
                        mapOf("rot" to rot, "barn" to barn, "barnebarn" to barnebarn, "sosken" to søsken, "annenRot" to annenRot),
                    ).asUpdate,
                )
            }

            runMigration()

            dbSession.session { session ->
                session
                    .run(
                        queryOf("SELECT behandling_id, behandlingskjede_id FROM behandling")
                            .map { it.uuid("behandling_id") to it.uuid("behandlingskjede_id") }
                            .asList,
                    ).toMap() shouldBe
                    mapOf(rot to rot, barn to rot, barnebarn to rot, søsken to rot, annenRot to annenRot)
            }
        }
    }
}
