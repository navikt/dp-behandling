package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.TransactionalSession

internal data class BatchStatement(private val query: String, private val params: List<Map<String, Any?>>) {
    fun run(tx: TransactionalSession) = tx.batchPreparedNamedStatement(query, params)
}
