package no.nav.dagpenger.mediator.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateOutput
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

data class DBTestContext(
    val baseConfig: HikariConfig,
) {
    val hikariConfig =
        HikariConfig().apply {
            baseConfig.copyStateTo(this)
            maximumPoolSize = 4
            minimumIdle = 0
        }

    val dbSession: DatabaseSession = DatabaseSession(lazy { HikariDataSource(hikariConfig) })

    // bruker lazy slik at migrering faktisk bare utføres én gang
    private val migratedOutput: List<MigrateOutput> by lazy {
        HikariDataSource(hikariConfig).use { flywayDataSource ->
            Flyway
                .configure()
                .connectRetries(10)
                .dataSource(flywayDataSource)
                .load()
                .migrate()
                .migrations
        }
    }

    fun runMigration(): Int = migratedOutput.size

    fun truncateTables() {
        dbSession.session { session ->
            val tabeller =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        select tablename from pg_tables
                        where schemaname = 'public' and tablename != 'flyway_schema_history'
                        """.trimIndent(),
                    ).map { it.string("tablename") }.asList,
                )

            if (tabeller.isEmpty()) return@session

            val tabellListe = tabeller.joinToString(", ") { """"public"."$it"""" }
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """truncate table $tabellListe restart identity cascade""",
                ).asExecute,
            )
        }
    }
}

private val ANTALL_TESTER_I_PARALLELL = System.getProperty("junit.jupiter.execution.parallel.config.fixed.parallelism")?.toInt() ?: 1

/**
 * oppretter en PostgreSQLContainer med flere databaser for å muliggjøre at tester kan kjøre i parallell.
 * databasene migreres med flyway én gang, og så tømmes tabellene mellom hver test.
 */
internal object Postgres {
    private val logger = KotlinLogging.logger { }
    private val instance by lazy {
        PostgreSQLContainer("postgres:18.0").apply {
            withReuse(true)
            start()
        }
    }

    private val systemtilkobling = instance.createConnection("")

    private val tilgjengeligeTestsesjoner =
        ArrayBlockingQueue(ANTALL_TESTER_I_PARALLELL, false, opprettInitielleTilkoblinger(ANTALL_TESTER_I_PARALLELL))

    fun withTestContext(block: (DBTestContext) -> Unit) {
        logger.info { "Tester venter på ledig database..." }
        val testContext =
            tilgjengeligeTestsesjoner.poll(Duration.ofSeconds(20).toSeconds(), TimeUnit.SECONDS) ?: error("Fikk ikke tak i databasesesjon!")
        try {
            logger.info { "Fikk tak i database..." }
            testContext.runMigration()
            block(testContext)
        } finally {
            logger.info { "Tømmer tabeller for innhold..." }
            testContext.truncateTables()
            logger.info { "Gir databasen tilbake..." }
            tilgjengeligeTestsesjoner.offer(testContext)
        }
    }

    fun withIsolatedTestContext(block: (DBTestContext) -> Unit) {
        val databasenavn = "testdb_isolated_${System.currentTimeMillis()}"
        val testContext = opprettTilkobling(databasenavn)
        try {
            block(testContext)
        } finally {
            logger.info { "Sletter midlertidig database" }
            systemtilkobling.createStatement().execute("drop database $databasenavn with (force)")
        }
    }

    private fun opprettInitielleTilkoblinger(antall: Int): List<DBTestContext> {
        logger.info { "Oppretter $antall testdatabaser..." }
        return (1..antall)
            .map { "testdb_$it" }
            .map { databasenavn -> opprettTilkobling(databasenavn) }
    }

    private fun opprettTilkobling(databasenavn: String): DBTestContext {
        logger.info { "Initialiserer $databasenavn" }
        opprettDatabase(databasenavn)
        logger.info { "oppretter testsesjon for $databasenavn" }
        return createTestSession(databasenavn)
    }

    private fun opprettDatabase(databasenavn: String) {
        if (finnesDatabase(databasenavn)) {
            logger.info { "oppretter ikke db for $databasenavn; finnes fra før" }
            return
        }
        logger.info { "oppretter db for $databasenavn" }
        systemtilkobling.createStatement().execute("create database $databasenavn")
    }

    private fun finnesDatabase(databasenavn: String): Boolean =
        systemtilkobling
            .prepareStatement(
                //language=PostgreSQL
                "SELECT EXISTS(SELECT 1 FROM pg_database WHERE datname = ?)",
            ).use { stmt ->
                stmt.setString(1, databasenavn)
                stmt.executeQuery().use {
                    it.next()
                    it.getBoolean(1)
                }
            }

    private fun createTestSession(databasenavn: String): DBTestContext {
        val connectionConfig =
            instance.withDatabaseName(databasenavn).let { instanceWithDbName ->
                HikariConfig().apply {
                    jdbcUrl = instanceWithDbName.jdbcUrl
                    username = instanceWithDbName.username
                    password = instanceWithDbName.password
                }
            }
        return DBTestContext(connectionConfig)
    }
}

internal inline fun withMigratedDb(crossinline block: DBTestContext.() -> Unit) {
    Postgres.withTestContext { context ->
        block(context)
    }
}

internal inline fun withIsolatedDb(crossinline block: DBTestContext.() -> Unit) {
    Postgres.withIsolatedTestContext { context ->
        block(context)
    }
}
