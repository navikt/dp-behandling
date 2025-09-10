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
    fun slettOpplysninger(antallBehandlinger: Int = 1): List<UUID> {
        var antallSlettet = 0

        val rapport =
            try {
                logger.info { "Skal finne kandidater til sletting, med øvre grense på $antallBehandlinger" }
                sessionOf(dataSource).use { session ->
                    logger.info { "Har opprettet session" }
                    logger.info { "Har startet transaksjon" }
                    logger.info { "Finner kandidater" }
                    val kandidater = session.hentOpplysningerSomErFjernet(antallBehandlinger)

                    logger.info {
                        "Fant ${kandidater.size} opplysningssett med ${
                            kandidater.sumOf {
                                it.opplysninger.size
                            }
                        } opplysninger til sletting"
                    }

                    kandidater.forEach { kandidat ->
                        withLoggingContext(
                            "behandlingId" to kandidat.behandlingId.toString(),
                            "opplysningerId" to kandidat.opplysningerId.toString(),
                        ) {
                            kandidat
                                .opplysninger
                                .asSequence()
                                .map { it }
                                .chunked(1000)
                                .forEach { batch ->
                                    session.transaction { tx ->
                                        tx.medLås(låsenøkkel) {
                                            try {
                                                logger.info { "Skal slette ${batch.size} opplysninger i batch" }

                                                val statements = mutableListOf<BatchStatement>()
                                                val params = batch.map { mapOf("id" to it) }

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
                                                antallSlettet += batch.size
                                            } catch (e: Exception) {
                                                logger.error(e) { "Feil ved sletting av opplysninger" }
                                            }
                                        }
                                        logger.info { "Slettet $antallSlettet opplysninger" }
                                    }
                                }
                        }
                    }
                    kandidater
                }
            } catch (e: Exception) {
                logger.error(e) { "Feil ved sletting av opplysninger" }
                null
            }
        return rapport?.flatMap { it.opplysninger } ?: emptyList()
    }

    internal data class Kandidat(
        val behandlingId: UUID?,
        val opplysningerId: UUID,
        val opplysninger: List<UUID> = emptyList(),
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

        val kandidaterMedOpplysninger =
            kandidater
                .map { kandidat ->
                    val opplysninger =
                        this.run(
                            queryOf(
                                query,
                                mapOf("opplysninger_id" to kandidat.opplysningerId),
                            ).map { row ->
                                row.uuid("id")
                            }.asList,
                        )
                    kandidat.copy(opplysninger = opplysninger)
                }

        if (kandidaterMedOpplysninger.isNotEmpty()) {
            logger.info {
                "Fant ${kandidater.size} opplysningsett for behandlinger ${
                    kandidater.map {
                        it.behandlingId
                    }
                } som inneholder ${
                    kandidaterMedOpplysninger.sumOf {
                        it.opplysninger.size
                    }
                } opplysninger som er fjernet og som skal slettes"
            }
        } else {
            logger.info { "Fant ingen kandidater til sletting" }
        }
        return kandidaterMedOpplysninger
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

        logger.info { "Hentet ut ${opplysningerIder.size} kandidater for sletting" }
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
