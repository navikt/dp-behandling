package no.nav.dagpenger.behandling.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

fun TransactionalSession.lås(nøkkel: Int) =
    run(
        queryOf(
            //language=PostgreSQL
            """
            SELECT PG_TRY_ADVISORY_LOCK(:key)
            """.trimIndent(),
            mapOf("key" to nøkkel),
        ).map { res ->
            res.boolean("pg_try_advisory_lock")
        }.asSingle,
    ) ?: false

fun TransactionalSession.låsOpp(nøkkel: Int) =
    run(
        queryOf(
            //language=PostgreSQL
            """
            SELECT PG_ADVISORY_UNLOCK(:key)
            """.trimIndent(),
            mapOf("key" to nøkkel),
        ).map { res ->
            res.boolean("pg_advisory_unlock")
        }.asSingle,
    ) ?: false

fun <T> TransactionalSession.medLås(
    nøkkel: Int,
    block: () -> T,
): T? {
    if (!lås(nøkkel)) {
        logger
            .warn { "Fikk ikke lås for $nøkkel" }
        hentLås()?.let {
            logger.warn {
                "Er allerede låst med $it"
            }
        } ?: logger
            .warn { "Fant ikke lås" }
        return null
    }
    return try {
        logger
            .info { "Fikk lås for $nøkkel" }
        block()
    } finally {
        logger
            .info { "Låser opp $nøkkel" }
        låsOpp(nøkkel)
    }
}

fun TransactionalSession.hentLås(): Pair<String, Int>? =
    run(
        queryOf(
            //language=PostgreSQL
            """
            SELECT mode,  objid FROM pg_locks WHERE locktype = 'advisory';
            """.trimIndent(),
        ).map { res ->
            Pair(res.string("mode"), res.int("objid"))
        }.asSingle,
    )
