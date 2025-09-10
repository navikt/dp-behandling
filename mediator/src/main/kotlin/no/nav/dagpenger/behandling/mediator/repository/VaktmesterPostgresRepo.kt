package no.nav.dagpenger.behandling.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.db.medLås
import java.util.UUID

internal class VaktmesterPostgresRepo {
    companion object {
        private val låsenøkkel = 121212
        private val logger = KotlinLogging.logger {}
    }

    @WithSpan
    fun slettOpplysninger(antall: Int = 1): List<UUID> {
        val slettet = mutableListOf<UUID>()
        try {
            sessionOf(dataSource).use { session ->
                val kandidater = session.hentOpplysningerSomErFjernet(antall)

                session.transaction { tx ->
                    kandidater.forEach { kandidat ->
                        tx.medLås(låsenøkkel) {
                            withLoggingContext(
                                "behandlingId" to kandidat.behandlingId.toString(),
                                "opplysningerId" to kandidat.opplysningerId.toString(),
                            ) {
                                val skalSlettes = kandidat.opplysninger().map { it.id }
                                val batchSize = 1000

                                for (i in skalSlettes.indices step batchSize) {
                                    val end = minOf(i + batchSize, skalSlettes.size)
                                    val opplysningIder = skalSlettes.subList(i, end)

                                    try {
                                        logger.info { "Skal slette ${opplysningIder.size} opplysninger " }

                                        val statements = mutableListOf<BatchStatement>()
                                        val params = opplysningIder.map { mapOf("id" to it) }

                                        // Slett erstatninger
                                        statements.add(slettErstatter(params))

                                        // Slett hvilke opplysninger som har vært brukt for å utlede opplysningen
                                        statements.add(slettOpplysningUtledetAv(params))

                                        // Slett hvilken regel som har vært brukt for å utlede opplysningen
                                        statements.add(slettOpplysningUtledning(params))

                                        // Slett verdien av opplysningen
                                        statements.add(slettOpplysningVerdi(params))

                                        // Fjern opplysningen fra opplysninger-settet
                                        statements.add(slettOpplysningLink(params))

                                        // Slett opplysningen
                                        statements.add(slettOpplysning(params))

                                        try {
                                            statements.forEach { batch ->
                                                batch.run(tx)
                                            }
                                        } catch (e: Exception) {
                                            throw IllegalStateException("Kunne ikke slette ", e)
                                        }
                                        slettet.addAll(opplysningIder)
                                        logger.info { "Slettet ${slettet.size} opplysninger" }
                                    } catch (e: Exception) {
                                        logger.error(e) { "Feil ved sletting av opplysninger" }
                                    }
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

        if (kandidater.isNotEmpty()) {
            logger.info {
                "Fant ${kandidater.size} opplysningsett for behandlinger ${
                    kandidater.map {
                        it.behandlingId
                    }
                } som inneholder $${kandidater.sumOf { it.opplysninger().size }} opplysninger som er fjernet og som skal slettes"
            }
        }
        return opplysninger
    }

    private fun Session.hentOpplysningerIder(antall: Int): List<Kandidat> {
        //language=PostgreSQL
        val query =
            """
            SELECT DISTINCT (op.opplysninger_id) AS opplysinger_id, b.behandling_id
            FROM opplysning
                INNER JOIN opplysninger_opplysning op ON opplysning.id = op.opplysning_id
                LEFT OUTER JOIN behandling_opplysninger b ON b.opplysninger_id = op.opplysninger_id
            WHERE fjernet = TRUE
            LIMIT :antall;
            """.trimIndent()

        val opplysningerIder =
            this.run(
                queryOf(
                    query,
                    mapOf(
                        "antall" to antall,
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

    private fun slettErstatter(params: List<Map<String, UUID>>) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_erstatter WHERE opplysning_id = :id
            """.trimIndent(),
            params,
        )

    private fun slettOpplysningVerdi(params: List<Map<String, UUID>>) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_verdi WHERE opplysning_id = :id
            """.trimIndent(),
            params,
        )

    private fun slettOpplysningLink(params: List<Map<String, UUID>>) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysninger_opplysning WHERE opplysning_id = :id
            """.trimIndent(),
            params,
        )

    private fun slettOpplysningUtledetAv(params: List<Map<String, UUID>>) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_utledet_av WHERE opplysning_id = :id OR utledet_av = :id
            """.trimIndent(),
            params,
        )

    private fun slettOpplysningUtledning(params: List<Map<String, UUID>>) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_utledning WHERE opplysning_id = :id
            """.trimIndent(),
            params,
        )

    private fun slettOpplysning(params: List<Map<String, UUID>>) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning WHERE id = :id
            """.trimIndent(),
            params,
        )
}
