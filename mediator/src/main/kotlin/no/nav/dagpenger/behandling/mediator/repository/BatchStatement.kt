package no.nav.dagpenger.behandling.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session

private val sikkerlogg = KotlinLogging.logger("tjenestekall.BatchStatement")

internal data class BatchStatement(
    private val query: String,
    private val params: List<Map<String, Any?>>,
) {
    fun run(tx: Session) =
        try {
            tx.batchPreparedNamedStatement(query, params)
        } catch (e: Exception) {
            sikkerlogg.error(e) { "Feil ved kjøring av batch statement: $query med params: $params" }
            throw e
        }
}
