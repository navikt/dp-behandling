package no.nav.dagpenger.behandling.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotliquery.TransactionalSession
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
        val rapport =
            try {
                logger.info { "Skal finne kandidater til sletting, med øvre grense på $antallBehandlinger" }
                sessionOf(dataSource).use { session ->
                    logger.info { "Har opprettet session" }
                    session.transaction { tx ->
                        logger.info { "Har startet transaksjon" }
                        tx.medLås(låsenøkkel) {
                            tx
                                .slettOpplysningerMerketForFjerning(antallBehandlinger)
                                .also(::loggSletting)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Feil ved sletting av opplysninger" }
                null
            }
        return rapport?.slettedeOpplysninger ?: emptyList()
    }

    private fun TransactionalSession.slettOpplysningerMerketForFjerning(antallBehandlinger: Int): SlettingRapport {
        logger.info { "Finner kandidater" }
        this
            .run(
                queryOf(
                    //language=PostgreSQL
                    """
                    CREATE TEMP TABLE opplysninger_til_sletting AS
                    with opplysningssett_som_skal_slettes as (
                        SELECT f.opplysninger_id, b.behandling_id
                        FROM (
                            SELECT DISTINCT o.opplysninger_id
                            FROM opplysning o
                            WHERE o.fjernet = TRUE
                        ) f
                        LEFT JOIN behandling_opplysninger b ON b.opplysninger_id = f.opplysninger_id
                        ORDER BY f.opplysninger_id
                        LIMIT :antall
                    )
                    SELECT o.id, o.opplysninger_id, oss.behandling_id
                    FROM opplysning o
                    inner join opplysningssett_som_skal_slettes oss on o.opplysninger_id = oss.opplysninger_id
                    WHERE fjernet = TRUE
                    ORDER BY o.opprettet DESC
                    """.trimIndent(),
                    mapOf("antall" to antallBehandlinger),
                ).asExecute,
            )

        logger.info { "sletter fra opplysning_utledet_av" }
        val antallSlettetUtledetAv =
            this
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        DELETE FROM opplysning_utledet_av oua
                        USING opplysninger_til_sletting ots
                        WHERE oua.opplysning_id = ots.id OR oua.utledet_av = ots.id
                        """.trimIndent(),
                    ).asUpdate,
                )

        logger.info { "sletter fra opplysning_utledning" }
        val antallSlettetUtledningAv =
            this
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        DELETE FROM opplysning_utledning ou
                        USING opplysninger_til_sletting ots
                        WHERE ou.opplysning_id = ots.id
                        """.trimIndent(),
                    ).asUpdate,
                )

        logger.info { "sletter fra opplysning" }
        val antallSlettetOpplysninger =
            this
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        DELETE FROM opplysning o
                        USING opplysninger_til_sletting ots
                        WHERE o.id = ots.id
                        """.trimIndent(),
                    ).asUpdate,
                )

        val antallOpplysningssett =
            this
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        select count(1) as antall from opplysninger_til_sletting group by opplysninger_id
                        """.trimIndent(),
                    ).map { row -> row.int("antall") }.asSingle,
                ) ?: 0

        val antallOpplysningerSomSkalSlettes =
            this
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        select count(1) as antall from opplysninger_til_sletting
                        """.trimIndent(),
                    ).map { row -> row.int("antall") }.asSingle,
                ) ?: 0

        val behandlinger =
            this
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT DISTINCT behandling_id
                        FROM opplysninger_til_sletting
                        WHERE behandling_id IS NOT NULL
                        """.trimIndent(),
                    ).map { row ->
                        row.uuid("behandling_id")
                    }.asList,
                )

        val opplysninger =
            this
                .run(
                    queryOf(
                        //language=PostgreSQL
                        """
                        SELECT id
                        FROM opplysninger_til_sletting
                        """.trimIndent(),
                    ).map { row ->
                        row.uuid("id")
                    }.asList,
                )

        this.run(
            queryOf(
                //language=PostgreSQL
                """drop table opplysninger_til_sletting""",
            ).asExecute,
        )

        return SlettingRapport(
            antallOpplysningssett = antallOpplysningssett,
            antallOpplysningerSomSkalSlettes = antallOpplysningerSomSkalSlettes,
            antallSlettetUtledetAv = antallSlettetUtledetAv,
            antallSlettetUtledning = antallSlettetUtledningAv,
            antallSlettetOpplysninger = antallSlettetOpplysninger,
            behandlinger = behandlinger,
            slettedeOpplysninger = opplysninger,
        )
    }

    private fun loggSletting(rapport: SlettingRapport) {
        check(rapport.antallOpplysningerSomSkalSlettes == rapport.antallSlettetOpplysninger) {
            "ulikt antall opplysninger som skulle slettes og faktisk slettede opplysninger, noe har gått galt"
        }
        logger.info { "Hentet ut ${rapport.antallOpplysningssett} kandidater for sletting" }
        if (rapport.antallOpplysningssett > 0) {
            logger.info {
                "Fant ${rapport.antallOpplysningssett} opplysningsett for behandlinger ${
                    rapport.behandlinger
                } som inneholder ${
                    rapport.antallOpplysningerSomSkalSlettes
                } opplysninger som er fjernet og som skal slettes"
            }
        } else {
            logger.info { "Fant ingen kandidater til sletting" }
        }

        logger.info {
            "Slettet ${rapport.antallSlettetOpplysninger} opplysninger, " +
                "${rapport.antallSlettetUtledetAv} utledet av og ${rapport.antallSlettetUtledning} utledninger, " +
                "fordelt på ${rapport.behandlinger.size} behandler og ${rapport.antallOpplysningssett} opplysningssett"
        }
    }

    private data class SlettingRapport(
        val antallOpplysningssett: Int,
        val antallOpplysningerSomSkalSlettes: Int,
        val antallSlettetUtledetAv: Int,
        val antallSlettetUtledning: Int,
        val antallSlettetOpplysninger: Int,
        val behandlinger: List<UUID>,
        val slettedeOpplysninger: List<UUID>,
    )
}
