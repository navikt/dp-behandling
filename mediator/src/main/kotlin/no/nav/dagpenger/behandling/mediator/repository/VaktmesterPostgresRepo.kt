package no.nav.dagpenger.behandling.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotliquery.Row
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

    private fun TransactionalSession.slettOpplysningerMerketForFjerning(antallBehandlinger: Int): SlettingRapport =
        this
            .run(
                queryOf(
                    //language=PostgreSQL
                    """
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
                    ),
                    opplysninger_som_skal_slettes as (
                        SELECT o.id
                        FROM opplysning o
                        WHERE fjernet = TRUE
                        AND o.opplysninger_id in (select opplysninger_id from opplysningssett_som_skal_slettes)
                        ORDER BY o.opprettet DESC
                    ),
                    slettet_utledet_av as (
                        DELETE
                        FROM opplysning_utledet_av oua
                        USING opplysninger_som_skal_slettes oss
                        WHERE oua.opplysning_id = oss.id OR oua.utledet_av = oss.id
                        RETURNING oua.opplysning_id
                    ),
                    slettet_utledning as (
                        DELETE FROM opplysning_utledning ou
                        USING opplysninger_som_skal_slettes oss
                        WHERE ou.opplysning_id = oss.id
                        RETURNING ou.opplysning_id
                    ),
                    slettet_opplysning as (
                        DELETE
                        FROM opplysning o
                        USING opplysninger_som_skal_slettes oss
                        WHERE o.id = oss.id
                        RETURNING o.id
                    )
                    select
                        (select count(1) from opplysningssett_som_skal_slettes) as antall_opplysningssett,
                        (select count(1) from opplysninger_som_skal_slettes) as antall_opplysninger_som_skal_slettes,
                        (select count(1) from slettet_utledet_av) as antall_slettet_utledet_av,
                        (select count(1) from slettet_utledning) as antall_slettet_utledning,
                        (select count(1) from slettet_opplysning) as antall_slettet_opplysninger,
                        (SELECT STRING_AGG(behandling_id::text, ',') FROM opplysningssett_som_skal_slettes where behandling_id is not null) AS behandlinger,
                        (SELECT STRING_AGG(id::text, ',') FROM slettet_opplysning) AS slettede_opplysninger
                    """.trimIndent(),
                    mapOf("antall" to antallBehandlinger),
                ).map(::mapSlettingRapport).asList,
            ).single()

    private fun mapSlettingRapport(row: Row): SlettingRapport =
        SlettingRapport(
            antallOpplysningssett = row.int("antall_opplysningssett"),
            antallOpplysningerSomSkalSlettes = row.int("antall_opplysninger_som_skal_slettes"),
            antallSlettetUtledetAv = row.int("antall_slettet_utledet_av"),
            antallSlettetUtledning = row.int("antall_slettet_utledning"),
            antallSlettetOpplysninger = row.int("antall_slettet_opplysninger"),
            behandlinger = row.splittTilUUID("behandlinger"),
            slettedeOpplysninger = row.splittTilUUID("slettede_opplysninger"),
        )

    private fun Row.splittTilUUID(kolonnenavn: String): List<UUID> =
        this
            .stringOrNull(kolonnenavn)
            ?.split(',')
            ?.map { UUID.fromString(it) }
            ?: emptyList()

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

        logger.info { "Slettet ${rapport.antallSlettetOpplysninger} opplysninger" }
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
