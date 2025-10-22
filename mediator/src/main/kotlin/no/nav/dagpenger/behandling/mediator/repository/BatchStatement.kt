package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Session

internal data class BatchStatement(private val query: String, private val params: List<Map<String, Any?>>) {
    fun run(tx: Session) = tx.batchPreparedNamedStatement(query, params)
}
