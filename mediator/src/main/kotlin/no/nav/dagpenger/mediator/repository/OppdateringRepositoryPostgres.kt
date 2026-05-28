package no.nav.dagpenger.mediator.repository

import kotliquery.queryOf
import no.nav.dagpenger.mediator.db.DatabaseSession
import java.util.UUID

internal data class NyOppdatering(
    val hendelseId: UUID,
    val ident: String,
    val behandlingId: UUID?,
    val type: String,
    val payload: String,
    val payloadHash: String,
)

internal data class OppdateringInnslag(
    val id: Long,
    val type: String,
    val payload: String,
)

internal interface OppdateringRepository {
    fun lagre(oppdateringer: List<NyOppdatering>)

    fun sisteIdForIdent(ident: String): Long

    fun sisteIdForBehandling(behandlingId: UUID): Long

    fun hentForIdent(
        ident: String,
        etterId: Long,
        limit: Int = 200,
    ): List<OppdateringInnslag>

    fun hentForBehandling(
        behandlingId: UUID,
        etterId: Long,
        limit: Int = 200,
    ): List<OppdateringInnslag>
}

internal class OppdateringRepositoryPostgres(
    private val dbSession: DatabaseSession,
) : OppdateringRepository {
    override fun lagre(oppdateringer: List<NyOppdatering>) {
        if (oppdateringer.isEmpty()) return
        dbSession.session { session ->
            session.batchPreparedNamedStatement(
                // language=PostgreSQL
                """
                INSERT INTO oppdatering_feed (hendelse_id, ident, behandling_id, type, payload, payload_hash)
                VALUES (:hendelseId, :ident, :behandlingId, :type, CAST(:payload AS jsonb), :payloadHash)
                ON CONFLICT (hendelse_id, type, payload_hash) DO NOTHING
                """.trimIndent(),
                oppdateringer.map {
                    mapOf(
                        "hendelseId" to it.hendelseId,
                        "ident" to it.ident,
                        "behandlingId" to it.behandlingId,
                        "type" to it.type,
                        "payload" to it.payload,
                        "payloadHash" to it.payloadHash,
                    )
                },
            )
        }
    }

    override fun hentForIdent(
        ident: String,
        etterId: Long,
        limit: Int,
    ): List<OppdateringInnslag> =
        dbSession.session { session ->
            session.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    SELECT id, type, payload::text AS payload
                    FROM oppdatering_feed
                    WHERE ident = :ident
                      AND id > :etterId
                    ORDER BY id ASC
                    LIMIT :limit
                    """.trimIndent(),
                    mapOf(
                        "ident" to ident,
                        "etterId" to etterId,
                        "limit" to limit,
                    ),
                ).map { row ->
                    OppdateringInnslag(
                        id = row.long("id"),
                        type = row.string("type"),
                        payload = row.string("payload"),
                    )
                }.asList,
            )
        }

    override fun sisteIdForIdent(ident: String): Long =
        dbSession.session { session ->
            session.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    SELECT COALESCE(MAX(id), 0) AS id
                    FROM oppdatering_feed
                    WHERE ident = :ident
                    """.trimIndent(),
                    mapOf("ident" to ident),
                ).map { row -> row.long("id") }.asSingle,
            ) ?: 0L
        }

    override fun hentForBehandling(
        behandlingId: UUID,
        etterId: Long,
        limit: Int,
    ): List<OppdateringInnslag> =
        dbSession.session { session ->
            session.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    SELECT id, type, payload::text AS payload
                    FROM oppdatering_feed
                    WHERE behandling_id = :behandlingId
                      AND id > :etterId
                    ORDER BY id ASC
                    LIMIT :limit
                    """.trimIndent(),
                    mapOf(
                        "behandlingId" to behandlingId,
                        "etterId" to etterId,
                        "limit" to limit,
                    ),
                ).map { row ->
                    OppdateringInnslag(
                        id = row.long("id"),
                        type = row.string("type"),
                        payload = row.string("payload"),
                    )
                }.asList,
            )
        }

    override fun sisteIdForBehandling(behandlingId: UUID): Long =
        dbSession.session { session ->
            session.run(
                queryOf(
                    // language=PostgreSQL
                    """
                    SELECT COALESCE(MAX(id), 0) AS id
                    FROM oppdatering_feed
                    WHERE behandling_id = :behandlingId
                    """.trimIndent(),
                    mapOf("behandlingId" to behandlingId),
                ).map { row -> row.long("id") }.asSingle,
            ) ?: 0L
        }
}
