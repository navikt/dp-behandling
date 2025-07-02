package no.nav.dagpenger.behandling.mediator.repository

import io.opentelemetry.instrumentation.annotations.WithSpan
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import mu.withLoggingContext
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
                                try {
                                    logger.info { "Skal slette ${kandidat.opplysninger().size} opplysninger " }

                                    kandidat.opplysninger().forEach { fjernetOpplysing ->
                                        val statements = mutableListOf<BatchStatement>()

                                        // Slett erstatninger
                                        statements.add(slettErstatter(fjernetOpplysing.id))

                                        // Slett hvilke opplysninger som har vært brukt for å utlede opplysningen
                                        statements.add(slettOpplysningUtledetAv(fjernetOpplysing.id))

                                        // Slett hvilken regel som har vært brukt for å utlede opplysningen
                                        statements.add(slettOpplysningUtledning(fjernetOpplysing.id))

                                        // Slett verdien av opplysningen
                                        statements.add(slettOpplysningVerdi(fjernetOpplysing.id))

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

    private fun slettErstatter(opplysningId: UUID) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_erstatter WHERE opplysning_id= :id
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

    private fun slettOpplysningUtledetAv(id: UUID) =
        BatchStatement(
            //language=PostgreSQL
            """
            DELETE FROM opplysning_utledet_av WHERE opplysning_id = :id OR utledet_av = :id
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
