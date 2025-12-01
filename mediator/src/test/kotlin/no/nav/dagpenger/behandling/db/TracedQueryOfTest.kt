package no.nav.dagpenger.behandling.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.http.decodeURLPart
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import org.junit.jupiter.api.Test

class TracedQueryOfTest {
    private val query = "SELECT 1"

    @Test
    fun `legger til sqlcommenter til spørringer på riktig måte`() {
        val tracedQuery =
            withSqlCommenter(
                query,
                mapOf(
                    "route" to "/polls 1000",
                    "name" to "'DROP TABLE FOO'",
                    "name''" to "\"DROP TABLE USERS'\"",
                ),
            )

        val (parsedSql, attrs) = parse(tracedQuery)
        parsedSql shouldBe query
        attrs shouldBe
            mapOf(
                "name" to "'DROP TABLE FOO'",
                "name''" to "\"DROP TABLE USERS'\"",
                "route" to "/polls 1000",
            )

        withMigratedDb {
            sessionOf(dataSource).use { session ->
                val count =
                    session.run(
                        queryOf(query).map { rs -> rs.int(1) }.asSingle,
                    )
                count shouldBe 1
            }
        }
    }

    @Test
    fun `queries som slutter med semikolon dævver`() =
        withMigratedDb {
            shouldThrow<IllegalArgumentException> {
                sessionOf(dataSource).use { session ->
                    session.run(
                        tracedQueryOf("SELECT 1;").map { rs -> rs.int(1) }.asSingle,
                    )
                }
            }
        }

    private fun parse(sql: String): Pair<String, Map<String, String>?> {
        if (!containsSqlComment(sql)) {
            return sql to null
        }

        // Since we now have a SQL comment, let's extract the serialized attributes.
        val (sqlStmt, serializedAttrs) = extractSqlCommenter(sql)

        if (serializedAttrs == null) {
            return sqlStmt to null
        }

        val pairs = serializedAttrs.split(",")
        val attrs =
            pairs.map { it.split("=") }.associate { (key, value) ->
                decodeKey(key) to decodeValue(value)
            }

        return sqlStmt to attrs
    }

    private fun containsSqlComment(sql: String) = sql.trim().matches(regex)

    private fun extractSqlCommenter(sql: String): Pair<String, String?> =
        sql.split("/*", limit = 2).let { parts ->
            val sqlPart = parts[0].trim()
            val commentPart = parts[1].removeSuffix("*/").trim()
            val serializedAttrs = commentPart.ifBlank { null }
            return sqlPart to serializedAttrs
        }

    private fun decodeKey(key: String) = key.replace("\'", "'").decodeURLPart()

    private fun decodeValue(value: String) = value.trim { it == '\'' }.replace("\'", "'").decodeURLPart()

    companion object {
        private val regex = Regex(".+/\\*.+\\*/$")
    }
}
