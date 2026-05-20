package no.nav.dagpenger.behandling.mediator.db

import ch.qos.logback.core.util.OptionHelper.getEnv
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.flywaydb.core.Flyway
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val DB_USERNAME_KEY = "DB_USERNAME"
private const val DB_PASSWORD_KEY = "DB_PASSWORD"
private const val DB_URL_KEY = "DB_URL"

// Understands how to create a data source from environment variables
internal class PostgresDataSourceBuilder {
    private fun getOrThrow(key: String): String = getEnv(key) ?: error("Mangler miljøvariabel $key")

    private val hikariConfig by lazy {
        HikariConfig().apply {
            jdbcUrl = getOrThrow(DB_URL_KEY).ensurePrefix("jdbc:postgresql://").stripCredentials()
            username = getOrThrow(DB_USERNAME_KEY)
            password = getOrThrow(DB_PASSWORD_KEY)
            // Default 10
            maximumPoolSize = 10
            // Default 30 sekund
            connectionTimeout = 10.seconds.inWholeMilliseconds
            // Default 10 minutter
            idleTimeout = 10.minutes.inWholeMilliseconds
            // Default 2 minutter
            keepaliveTime = 2.minutes.inWholeMilliseconds
            // Default 30 minutter
            maxLifetime = 30.minutes.inWholeMilliseconds
            leakDetectionThreshold = 10.seconds.inWholeMilliseconds
            metricRegistry =
                PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    PrometheusRegistry.defaultRegistry,
                    Clock.SYSTEM,
                )
        }
    }

    val dataSource by lazy { HikariDataSource(hikariConfig) }

    internal fun runMigration() {
        Flyway
            .configure()
            .connectRetries(10)
            .dataSource(dataSource)
            .load()
            .migrate()
    }
}

private fun String.stripCredentials() = this.replace(Regex("://.*:.*@"), "://")

private fun String.ensurePrefix(prefix: String) =
    if (this.startsWith(prefix)) {
        this
    } else {
        prefix + this.substringAfter("//")
    }
