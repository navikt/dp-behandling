package no.nav.dagpenger.behandling.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.behandling.mediator.db.DatabaseSession
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
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

private val ANTALL_TESTER_I_PARALLELL = System.getProperty("junit.jupiter.execution.parallel.config.fixed.parallelism")?.toInt() ?: 1

internal object Postgres {
    private val logger = KotlinLogging.logger { }
    private val instance by lazy {
        PostgreSQLContainer("postgres:18.0").apply {
            withReuse(true)
            start()
        }
    }

    private val tilgjengeligeTestsesjoner =
        ArrayBlockingQueue(ANTALL_TESTER_I_PARALLELL, false, opprettInitielleTilkoblinger(ANTALL_TESTER_I_PARALLELL))

    fun withTestContext(block: (DBTestContext) -> Unit) {
        logger.info { "Tester venter på ledig database..." }
        val testContext =
            tilgjengeligeTestsesjoner.poll(Duration.ofSeconds(20).toSeconds(), TimeUnit.SECONDS) ?: error("Fikk ikke tak i databasesesjon!")
        try {
            logger.info { "Fikk tak i database..." }
            block(testContext)
        } finally {
            logger.info { "Gir databasen tilbake..." }
            tilgjengeligeTestsesjoner.offer(testContext)
        }
    }

    private fun opprettInitielleTilkoblinger(antall: Int): List<DBTestContext> =
        (1..antall)
            .map { "testdb_$it" }
            .map { databasenavn ->
                logger.info { "Initialiserer $databasenavn" }
                logger.info { "oppretter db for $databasenavn" }
                instance.createConnection("").use { conn ->
                    conn.createStatement().execute("create database $databasenavn")
                }
                logger.info { "oppretter testsesjon for $databasenavn" }
                createTestSession(instance.withDatabaseName(databasenavn))
            }

    private fun createTestSession(instance: PostgreSQLContainer): DBTestContext {
        val hikariConfig =
            HikariConfig().apply {
                jdbcUrl = instance.jdbcUrl
                username = instance.username
                password = instance.password
                maximumPoolSize = 4
            }
        val flywayConfig =
            HikariConfig().apply {
                hikariConfig.copyStateTo(this)
            }

        val dataSource = lazy { HikariDataSource(hikariConfig) }
        val flywayDataSource = lazy { HikariDataSource(flywayConfig) }

        return DBTestContext(DatabaseSession(dataSource), flywayDataSource)
    }
}

internal inline fun withMigratedDb(crossinline block: DBTestContext.() -> Unit) {
    withCleanDb {
        runMigration()
        block()
    }
}

internal inline fun withCleanDb(crossinline block: DBTestContext.() -> Unit) {
    Postgres.withTestContext { context ->
        context.clean()
        block(context)
    }
}
