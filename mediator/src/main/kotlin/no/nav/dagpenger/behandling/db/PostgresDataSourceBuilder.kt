package no.nav.dagpenger.behandling.db

import ch.qos.logback.core.util.OptionHelper.getEnv
import ch.qos.logback.core.util.OptionHelper.getSystemProperty
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.encodeURLParameter
import io.opentelemetry.api.trace.Span
import kotliquery.Query
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.internal.configuration.ConfigUtils
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Understands how to create a data source from environment variables
internal object PostgresDataSourceBuilder {
    const val DB_USERNAME_KEY = "DB_USERNAME"
    const val DB_PASSWORD_KEY = "DB_PASSWORD"
    const val DB_URL_KEY = "DB_URL"

    private fun getOrThrow(key: String): String = getEnv(key) ?: getSystemProperty(key)

    val dataSource by lazy {
        HikariDataSource().apply {
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
        }
    }

    private fun flyWayBuilder() = Flyway.configure().validateMigrationNaming(true).connectRetries(10)

    private val flyWayBuilder: FluentConfiguration = Flyway.configure().connectRetries(10)

    fun clean() =
        flyWayBuilder
            .cleanDisabled(
                getOrThrow(ConfigUtils.CLEAN_DISABLED).toBooleanStrict(),
            ).dataSource(dataSource)
            .load()
            .clean()

    internal fun runMigration(initSql: String? = null): Int =
        flyWayBuilder
            .dataSource(dataSource)
            .initSql(initSql)
            .load()
            .migrate()
            .migrations
            .size

    internal fun runMigrationTo(target: String): Int =
        flyWayBuilder()
            .dataSource(dataSource)
            .target(target)
            .load()
            .migrate()
            .migrations
            .size
}

private fun String.stripCredentials() = this.replace(Regex("://.*:.*@"), "://")

private fun String.ensurePrefix(prefix: String) =
    if (this.startsWith(prefix)) {
        this
    } else {
        prefix + this.substringAfter("//")
    }

fun withSqlCommenter(
    sql: String,
    metadata: Map<String, String?>,
): String {
    require(!sql.contains(";")) { "SQL kan ikke inneholde semikolon når SQL commenter brukes." }
    val filtered = metadata.filter { it.value?.isNotBlank() == true }

    val encoded =
        filtered
            .mapKeys { (k, v) ->
                val encoded = k.encodeURLParameter()
                val escaped = encoded.replace("'", "\'")
                escaped
            }.mapValues { (_, v) ->
                val encoded = v?.encodeURLParameter()
                val escaped = encoded?.replace("'", "\'")
                val final = "'$escaped'"
                final
            }.entries
            .sortedBy { it.key }
            .joinToString(",") { (k, v) ->
                "$k=$v"
            }
    return "$sql /*$encoded*/"
}

private fun tracedQuery(sql: String): String {
    val span = Span.current()
    val traceId = span.spanContext.traceId
    val spanId = span.spanContext.spanId
    return withSqlCommenter(
        sql,
        mapOf(
            "trace_id" to traceId,
            "span_id" to spanId,
        ),
    )
}

fun tracedQueryOf(
    statement: String,
    paramMap: Map<String, Any?>,
): Query = Query(tracedQuery(statement), paramMap = paramMap)

fun tracedQueryOf(
    statement: String,
    vararg params: Any?,
): Query = Query(tracedQuery(statement), params = params.toList())
