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
                    session.transaction { tx ->
                        logger.info { "Har startet transaksjon" }
                        tx.medLås(låsenøkkel) {
                            logger.info { "Finner kandidater" }
                            val kandidater = tx.hentOpplysningerSomErFjernet(antallBehandlinger)

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
                                    try {
                                        logger.info { "Skal slette ${kandidat.opplysninger.size} opplysninger" }

                                        val statements = mutableListOf<BatchStatement>()
                                        val params = kandidat.opplysninger.map { mapOf("id" to it) }

                                        // Slett erstatninger
                                        // statements.add(slettErstatter(params))

                                        // Slett hvilke opplysninger som har vært brukt for å utlede opplysningen
                                        statements.add(slettOpplysningUtledetAv(params))

                                        // Slett hvilken regel som har vært brukt for å utlede opplysningen
                                        statements.add(slettOpplysningUtledning(params))

                                        // Slett verdien av opplysningen
                                        // statements.add(slettOpplysningVerdi(params))

                                        // Fjern opplysningen fra opplysninger-settet
                                        // statements.add(slettOpplysningLink(params))

                                        // Slett opplysningen
                                        statements.add(slettOpplysning(params))

                                        try {
                                            statements.forEach { batch ->
                                                batch.run(tx)
                                            }
                                        } catch (e: Exception) {
                                            throw IllegalStateException("Kunne ikke slette ", e)
                                        }
                                        antallSlettet += kandidat.opplysninger.size
                                    } catch (e: Exception) {
                                        logger.error(e) { "Feil ved sletting av opplysninger" }
                                    }
                                }
                            }
                            logger.info { "Slettet $antallSlettet opplysninger" }
                            kandidater
                        }
                    }
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

    private fun Session.hentOpplysningerIder(antall: Int): List<Kandidat> {
        val opplysningerIder =
            this.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT f.opplysninger_id, b.behandling_id
                    FROM (
                        SELECT DISTINCT o.opplysninger_id
                        FROM opplysning o 
                        WHERE o.fjernet = TRUE
                    ) f
                    LEFT JOIN behandling_opplysninger b ON b.opplysninger_id = f.opplysninger_id
                    ORDER BY f.opplysninger_id
                    LIMIT ? 
                    """.trimIndent(),
                    antall,
                ).map { row ->
                    Kandidat(
                        row.uuidOrNull("behandling_id"),
                        row.uuid("opplysninger_id"),
                    )
                }.asList,
            )

        logger.info { "Hentet ut ${opplysningerIder.size} kandidater for sletting" }
        return opplysningerIder
    }

    private fun Session.hentOpplysningerSomErFjernet(antall: Int): List<Kandidat> {
        val kandidater = this.hentOpplysningerIder(antall)

        val kandidaterMedOpplysninger =
            kandidater
                .map { kandidat ->
                    val opplysninger =
                        this.run(
                            queryOf(
                                //language=PostgreSQL
                                """
                                SELECT o.id
                                FROM opplysning o 
                                WHERE fjernet = TRUE AND o.opplysninger_id = :opplysninger_id
                                ORDER BY o.opprettet DESC
                                """.trimIndent(),
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
