package no.nav.dagpenger.behandling.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.dagpenger.behandling.mediator.db.DatabaseSession
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer

data class DBTestContext(
    val dbSession: DatabaseSession,
    val flyWay: Flyway,
) {
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
    val instance by lazy {
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

    private val flywayDataSource by lazy { HikariDataSource(flywayConfig) }

    private val flyWay by lazy {
        Flyway
            .configure()
            .connectRetries(10)
            .dataSource(flywayDataSource)
            .cleanDisabled(false)
            .load()
    }

    inline fun withMigratedDb(block: DBTestContext.() -> Unit) {
        withCleanDb {
            runMigration()
            block()
        }
    }

    inline fun withCleanDb(block: DBTestContext.() -> Unit) {
        val context = DBTestContext(DatabaseSession(lazy { HikariDataSource(hikariConfig) }), flyWay)
        context.clean()
        block(context)
    }
}
