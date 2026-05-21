package no.nav.dagpenger.behandling.db

import io.kotest.matchers.ints.shouldBeExactly
import org.junit.jupiter.api.Test

class PostgresMigrationTest {
    @Test
    fun `Migration scripts are applied successfully`() {
        withCleanDb {
            val migrations = runMigration()
            migrations shouldBeExactly 25
        }
    }
}
