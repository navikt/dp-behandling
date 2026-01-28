package no.nav.dagpenger.behandling.db

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.internal.configuration.ConfigUtils
import org.testcontainers.postgresql.PostgreSQLContainer
import javax.sql.DataSource

data class DBTestContext(
    val dataSource: DataSource,
)

internal object Postgres {
    val instance by lazy {
        PostgreSQLContainer("postgres:18.0").apply {
            withReuse(true)
            start()
        }
    }

    fun withMigratedDb(block: DBTestContext.() -> Unit) {
        withCleanDb {
            PostgresDataSourceBuilder.runMigration()
            block(this)
        }
    }

    fun withMigratedDb(
        target: String,
        block: DBTestContext.() -> Unit,
    ) {
        withCleanDb {
            PostgresDataSourceBuilder.runMigrationTo(target)
            block(this)
        }
    }

    fun withMigratedDb(): HikariDataSource {
        setup()
        PostgresDataSourceBuilder.runMigration()
        return PostgresDataSourceBuilder.dataSource1
    }

    fun setup() {
        System.setProperty(ConfigUtils.CLEAN_DISABLED, "false")
        System.setProperty(PostgresDataSourceBuilder.DB_URL_KEY, instance.jdbcUrl)
        System.setProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY, instance.username)
        System.setProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY, instance.password)
    }

    fun tearDown() {
        System.clearProperty(PostgresDataSourceBuilder.DB_URL_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY)
        System.clearProperty(ConfigUtils.CLEAN_DISABLED)
    }

    fun withCleanDb(block: DBTestContext.() -> Unit) {
        setup()
        PostgresDataSourceBuilder
            .clean()
            .run {
                val testContext = DBTestContext(PostgresDataSourceBuilder.dataSource1)
                block(testContext)
            }.also {
                tearDown()
            }
    }

    fun withCleanDb(
        target: String,
        setup: () -> Unit,
        test: () -> Unit,
    ) {
        this.setup()
        PostgresDataSourceBuilder
            .clean()
            .run {
                PostgresDataSourceBuilder.runMigrationTo(target)
                setup()
                PostgresDataSourceBuilder.runMigration()
                test()
            }.also {
                tearDown()
            }
    }
}
