package no.nav.dagpenger.behandling.db

import io.kotest.matchers.ints.shouldBeExactly
import no.nav.dagpenger.behandling.db.Postgres.withCleanDb
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.runMigration
import org.junit.jupiter.api.Test

class PostgresMigrationTest {
    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = runMigration()
            migrations shouldBeExactly 67
        }
    }
}
