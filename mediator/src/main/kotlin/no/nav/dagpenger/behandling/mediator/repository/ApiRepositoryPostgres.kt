package no.nav.dagpenger.behandling.mediator.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.ForslagTilVedtak
import no.nav.dagpenger.behandling.modell.Behandling.TilstandType.TilGodkjenning
import java.util.UUID
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class ApiRepositoryPostgres(
    private val timout: Duration = 15.seconds,
    private val pollIntervalMs: Duration = 50.milliseconds,
) {
    fun behovLøst(
        behandlingId: UUID,
        vararg behov: String,
    ) {
        logger.info { "Markerer behov som løst for behandlingId=$behandlingId, behov=${behov.joinToString()}" }
        sessionOf(dataSource).use { session ->
            session.transaction {
                queryOf(
                    // language=PostgreSQL
                    """
                    DELETE
                    FROM behandling_aktive_behov
                    WHERE behandling_id = :behandlingId AND behov = :behov
                    """.trimIndent(),
                    mapOf("behandlingId" to behandlingId, "behov" to behov),
                ).asUpdate
            }
        }
    }

    suspend fun endreOpplysning(
        behandlingId: UUID,
        behov: String,
        block: () -> Unit,
    ) {
        // 1. Insert an "active change" row in a separate transaction.
        logger.info { "Oppretter behov som uløst for behandlingId=$behandlingId, behov=$behov" }
        sessionOf(dataSource).use { session ->
            session.transaction {
                queryOf(
                    // language=PostgreSQL
                    """
                    INSERT INTO behandling_aktive_behov (behandling_id, behov, status, opprettet)
                    VALUES (:behandlingId, :behov, 'pending', NOW())
                    """.trimIndent(),
                    mapOf("behandlingId" to behandlingId, "behov" to behov),
                ).asUpdate
            }
        }

        // 2. Execute the block that publishes to Kafka.
        try {
            logger.info { "Utfører endring for behandlingId=$behandlingId, behov=$behov" }
            block()
        } catch (e: Exception) {
            logger.info { "Fikk feil under endring for behandlingId=$behandlingId, behov=$behov" }
            // If Kafka fails to produce, update the active change row to mark as failed.
            sessionOf(dataSource).use { session ->
                session.transaction {
                    queryOf(
                        // language=PostgreSQL
                        """
                        UPDATE behandling_aktive_behov
                        SET status = 'failed'
                        WHERE behandling_id = :behandlingId AND behov = :behov
                        """.trimIndent(),
                        mapOf("behandlingId" to behandlingId, "behov" to behov),
                    ).asUpdate
                }
            }
            throw e
        }

        logger.info { "Venter på ferdig endring for behandlingId=$behandlingId, behov=$behov" }
        // 3. Poll until the active change row is removed or marked as 'completed'.
        if (!ventEndringFerdig(behandlingId, behov)) {
            logger.info { "Endring timeout for behandlingId=$behandlingId, behov=$behov" }
            throw TimeoutException("Active change not completed in time for behandlingId: $behandlingId")
        }

        logger.info { "Venter på riktig tilstand for behandlingId=$behandlingId, behov=$behov" }
        // 4. Poll until the aggregate reaches the desired state.
        if (!ventBehandlingTilstand(behandlingId)) {
            logger.info { "Tilstand timeout for behandlingId=$behandlingId, behov=$behov" }
            throw TimeoutException("Aggregate did not reach desired state for behandlingId: $behandlingId")
        }
    }

    /** Polls the active_changes table for removal or a status change */
    private suspend fun ventEndringFerdig(
        behandlingId: UUID,
        behov: String,
    ) = try {
        withTimeout(timout) {
            while (getStatus(behandlingId, behov) != null) {
                delay(pollIntervalMs)
            }
            return@withTimeout true
        }
    } catch (e: TimeoutException) {
        false
    }

    private suspend fun ventBehandlingTilstand(behandlingId: UUID) =
        try {
            withTimeout(timout) {
                while (hentBehandlingTilstand(behandlingId)?.erFerdig() != true) {
                    delay(pollIntervalMs)
                }
                return@withTimeout true
            }
        } catch (e: TimeoutException) {
            false
        }

    private fun getStatus(
        behandlingId: UUID,
        behov: String,
    ) = sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                // language=PostgreSQL
                """
                SELECT status
                FROM behandling_aktive_behov
                WHERE behandling_id = :behandlingId AND behov = :behov
                """.trimIndent(),
                mapOf("behandlingId" to behandlingId, "behov" to behov),
            ).map { it.string("status") }.asSingle,
        )
    }

    private fun hentBehandlingTilstand(behandlingId: UUID) =
        sessionOf(dataSource).use { session ->
            session
                .run(
                    queryOf(
                        // language=PostgreSQL
                        """
                        SELECT tilstand
                        FROM behandling
                        WHERE behandling_id = :behandlingId
                        """.trimIndent(),
                        mapOf("behandlingId" to behandlingId),
                    ).map { TilstandType.valueOf(it.string("tilstand")) }.asSingle,
                ).also {
                    logger.info { "Hentet ut tilstand=${it?.name} for behandlingId=$behandlingId" }
                }
        }

    private fun TilstandType.erFerdig(): Boolean = this == ForslagTilVedtak || this == TilGodkjenning
}
