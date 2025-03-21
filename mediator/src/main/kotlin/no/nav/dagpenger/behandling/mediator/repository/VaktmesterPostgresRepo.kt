package no.nav.dagpenger.behandling.mediator.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class VaktmesterPostgresRepo {
    companion object {
        val låsenøkkel = 121212
        private val logger = KotlinLogging.logger {}
        private val skipOpplysninger =
            setOf(
                "01932f46-c4d3-755e-a4da-c572945a93b5",
                "0193d9ac-aaec-7fd1-8ca1-f19576afb041",
                "0193d9ab-e8d4-77cc-a475-c3765a6d14d0",
                "0193d9d1-347d-7e44-8690-27a44726ca53",
                "0193d9d1-1c89-70e4-80a0-b2f2ec36e036",
                "0193d9ab-e8d4-77cc-a475-c3765a6d14d0",
                "0193aa6d-feaf-7851-aa1d-9df48cf9682a",
                "0193aa6b-693a-7344-8f40-5bef03b3a2d1",
                "0193aa68-d992-70f2-ab7a-eeba72abb20e",
                "0193aa64-a5a0-7d46-a775-996dbc67b8f5",
                "0193aa61-1a3c-76df-b543-fc8e134f4299",
                "0193aa5f-2d49-7be4-b42d-afc875df1458",
                "0193aa5c-314b-7b70-bd00-766b012d252f",
                "019391b4-73e7-7512-847f-73a1f805ab80",
                "019391c0-d9f5-78d9-8704-02f749bfb17c",
                "0193ca27-38f1-7887-a8f1-7e767d652cb4",
                "019368a2-a33b-7c91-8579-4334ca134884",
                "01934fc6-09c4-711c-8da1-21e331aeec54",
                "01944fe4-e98b-7d9a-8933-6fbeda345708",
                "01945074-7724-72c7-a17d-444946c74ae7",
            )
    }

    @WithSpan
    fun slettOpplysninger(antall: Int = 1): List<UUID> {
        val slettet = mutableListOf<UUID>()
        try {
            sessionOf(dataSource).use { session ->
                session.transaction { tx ->
                    tx.medLås(låsenøkkel) {
                        val kandidater = tx.hentOpplysningerSomErFjernet(antall)
                        kandidater.forEach { kandidat ->
                            withLoggingContext(
                                "behandlingId" to kandidat.behandlingId.toString(),
                                "opplysningerId" to kandidat.opplysningerId.toString(),
                            ) {
                                try {
                                    logger.info { "Skal slette ${kandidat.opplysninger().size} opplysninger " }
                                    kandidat.opplysninger().forEach { fjernetOpplysing ->
                                        val statements = mutableListOf<BatchStatement>()

                                        // Slett hvilke opplysninger som har vært brukt for å utlede opplysningen
                                        statements.add(slettOpplysningUtledet(fjernetOpplysing.id))

                                        // Slett hvilken regel som har vært brukt for å utlede opplysningen
                                        statements.add(slettOpplysningUtledning(fjernetOpplysing.id))

                                        // Slett verdien av opplysningen
                                        statements.add(slettOpplysningVerdi(fjernetOpplysing.id))

                                        // Slett erstatninger
                                        statements.add(slettErstatteAv(fjernetOpplysing.id))

                                        // Fjern opplysningen fra opplysninger-settet
                                        statements.add(slettOpplysningLink(fjernetOpplysing.id))

                                        // Slett opplysningen
                                        statements.add(slettOpplysning(fjernetOpplysing.id))

                                        try {
                                            statements.forEach { batch ->
                                                batch.run(tx)
                                            }
                                        } catch (e: Exception) {
                                            throw IllegalStateException("Kunne ikke slette $fjernetOpplysing", e)
                                        }
                                        slettet.add(fjernetOpplysing.id)
                                    }
                                    logger.info { "Slettet ${kandidat.opplysninger().size} opplysninger" }
                                } catch (e: Exception) {
                                    logger.error(e) { "Feil ved sletting av opplysninger" }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Feil ved sletting av opplysninger" }
        }
        return slettet
    }

    internal data class Kandidat(
        val behandlingId: UUID?,
        val opplysningerId: UUID,
        private val opplysninger: MutableList<FjernetOpplysing> = mutableListOf(),
    ) {
        fun leggTil(fjernet: FjernetOpplysing) {
            opplysninger.add(fjernet)
        }

        fun opplysninger() = opplysninger.toList()
    }

    internal data class FjernetOpplysing(
        val id: UUID,
        val navn: String,
        val opplysningstypeId: UUID,
    )

    private fun Session.hentOpplysningerSomErFjernet(antall: Int): List<Kandidat> {
        val kandidater = this.hentOpplysningerIder(antall)

        //language=PostgreSQL
        val query =
            """
            SELECT id, navn, uuid
            FROM opplysning
            INNER JOIN opplysningstype ON opplysning.opplysningstype_id = opplysningstype.opplysningstype_id
            INNER JOIN opplysninger_opplysning op ON opplysning.id = op.opplysning_id
            WHERE fjernet = TRUE AND op.opplysninger_id = :opplysninger_id
            ORDER BY op.opplysninger_id, opplysning.opprettet DESC;
            """.trimIndent()

        val opplysninger =
            kandidater
                .onEach { kandidat ->
                    this.run(
                        queryOf(
                            query,
                            mapOf("opplysninger_id" to kandidat.opplysningerId),
                        ).map { row ->
                            kandidat.leggTil(
                                FjernetOpplysing(
                                    row.uuid("id"),
                                    row.string("navn"),
                                    row.uuid("uuid"),
                                ),
                            )
                        }.asList,
                    )
                }
        val antallOpplysinger: Int =
            kandidater
                .takeIf { it.isNotEmpty() }
                ?.map {
                    it.opplysninger().size
                }?.reduce { acc, i -> acc + i } ?: 0
        logger.info {
            "Fant ${kandidater.size} opplysningsett for behandlinger ${
                kandidater.map {
                    it.behandlingId
                }
            } som inneholder $antallOpplysinger opplysninger som er fjernet og som skal slettes"
        }
        return opplysninger
    }

    private fun Session.hentOpplysningerIder(antall: Int): List<Kandidat> {
        //language=PostgreSQL
        val test =
            """
            SELECT DISTINCT (op.opplysninger_id) AS opplysinger_id, b.behandling_id
            FROM opplysning
                INNER JOIN opplysninger_opplysning op ON opplysning.id = op.opplysning_id
                LEFT OUTER JOIN behandling_opplysninger b ON b.opplysninger_id = op.opplysninger_id
            WHERE fjernet = TRUE AND op.opplysninger_id NOT IN (SELECT UNNEST(:skip_opplysninger::uuid[]))
            LIMIT :antall;
            """.trimIndent()

        val opplysningerIder =
            this.run(
                queryOf(
                    test,
                    mapOf(
                        "antall" to antall,
                        "skip_opplysninger" to skipOpplysninger.toTypedArray(),
                    ),
                ).map { row ->
                    Kandidat(
                        row.uuidOrNull("behandling_id"),
                        row.uuid("opplysinger_id"),
                    )
                }.asList,
            )
        return opplysningerIder
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
        logger.warn { "Fikk ikke lås for $nøkkel" }
        hentLås()?.let {
            logger.warn {
                "Er allerede låst med $it"
            }
        } ?: logger.warn { "Fant ikke lås" }
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
