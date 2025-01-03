package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class VaktmesterPostgresRepo {
    companion object {
        val låsenøkkel = 121212
    }

    fun slettOpplysninger(): List<UUID> {
        val antall = mutableListOf<UUID>()
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                tx.medLås(låsenøkkel) {
                    hentAlleOpplysningerSomErFjernet(tx).forEach { opplysningId ->
                        slettOpplysningVerdi(opplysningId).run(tx)
                        slettOpplysningUtledet(opplysningId).run(tx)
                        slettOpplysningLink(opplysningId).run(tx)
                        slettOpplysningUtledning(opplysningId).run(tx)
                        slettErstatteAv(opplysningId).run(tx)
                        slettOpplysning(opplysningId).run(tx)
                        antall.add(opplysningId)
                    }
                }
            }
        }
        return antall
    }

    private fun hentAlleOpplysningerSomErFjernet(tx: TransactionalSession): List<UUID> {
        //language=PostgreSQL
        val query =
            """
            SELECT *
            FROM opplysning
            WHERE fjernet = true
            ORDER BY opprettet DESC;
            """.trimIndent()
        val opplysninger =
            tx.run(
                queryOf(
                    query,
                    mapOf("fjernet" to true),
                ).map { row ->
                    row.uuid("id")
                }.asList,
            )
        logger.info { "Fant ${opplysninger.size} opplysninger som er fjernet og som skal slettes" }
        return opplysninger
    }

    private fun slettErstatteAv(opplysningId: UUID) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_erstattet_av WHERE erstattet_av = :id
            """.trimIndent(),
            listOf(mapOf("id" to opplysningId)),
        )

    private fun slettOpplysningVerdi(id: UUID) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_verdi WHERE opplysning_id = :id
            """.trimIndent(),
            listOf(mapOf("id" to id)),
        )

    private fun slettOpplysningLink(id: UUID) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysninger_opplysning WHERE opplysning_id = :id
            """.trimIndent(),
            listOf(mapOf("id" to id)),
        )

    private fun slettOpplysningUtledet(id: UUID) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_utledet_av WHERE opplysning_id = :id
            """.trimIndent(),
            listOf(mapOf("id" to id)),
        )

    private fun slettOpplysningUtledning(id: UUID) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_utledning WHERE opplysning_id = :id
            """.trimIndent(),
            listOf(mapOf("id" to id)),
        )

    private fun slettOpplysning(id: UUID) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning WHERE id = :id
            """.trimIndent(),
            listOf(mapOf("id" to id)),
        )
}

fun Session.lås(nøkkel: Int) =
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

fun Session.låsOpp(nøkkel: Int) =
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

fun <T> Session.medLås(
    nøkkel: Int,
    block: () -> T,
): T? {
    if (!lås(nøkkel)) {
        logger.warn { "Fikk ikke lås for $nøkkel" }
        return null
    }
    return try {
        logger.info { "Fikk lås for $nøkkel" }
        block()
    } finally {
        logger.info { "Låser opp $nøkkel" }
        låsOpp(nøkkel)
    }
}
