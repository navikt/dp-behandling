package no.nav.dagpenger.mediator.db

import io.kotest.matchers.ints.shouldBeExactly
import org.junit.jupiter.api.Test

class PostgresMigrationTest {
    @Test
    fun `Migration scripts are applied successfully`() {
        withIsolatedDb {
            val migrations = runMigration()
            migrations shouldBeExactly 27
        }
    }
}
