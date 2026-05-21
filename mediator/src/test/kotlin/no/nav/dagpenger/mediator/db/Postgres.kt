package no.nav.dagpenger.mediator.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.dagpenger.mediator.db.DatabaseSession
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer
import javax.sql.DataSource

data class DBTestContext(
    val dbSession: DatabaseSession,
    private val flywayDataSource: Lazy<DataSource>,
) {
    private val flyWay by lazy {
        Flyway
            .configure()
            .connectRetries(10)
            .dataSource(flywayDataSource.value)
            .cleanDisabled(false)
            .load()
    }

    fun clean() {
        flyWay.clean()
    }

    fun runMigration(): Int =
        flyWay
            .migrate()
            .migrations
            .size
}

internal object Postgres {
    private val instance by lazy {
        PostgreSQLContainer("postgres:18.0").apply {
            withReuse(true)
            start()
        }
    }

    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = instance.jdbcUrl
            username = instance.username
            password = instance.password
        }
    private val flywayConfig =
        HikariConfig().apply {
            hikariConfig.copyStateTo(this)
        }

    private val dataSource = lazy { HikariDataSource(hikariConfig) }
    private val flywayDataSource = lazy { HikariDataSource(flywayConfig) }

    val testContext = DBTestContext(DatabaseSession(dataSource), flywayDataSource)
}

internal inline fun withMigratedDb(block: DBTestContext.() -> Unit) {
    withCleanDb {
        runMigration()
        block()
    }
}

internal inline fun withCleanDb(block: DBTestContext.() -> Unit) {
    val context = Postgres.testContext
    context.clean()
    block(context)
}
