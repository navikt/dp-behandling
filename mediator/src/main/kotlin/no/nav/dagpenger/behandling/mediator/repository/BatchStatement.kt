package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Session

internal data class BatchStatement(
    private val query: String,
    private val params: List<Map<String, Any?>>,
) {
    fun run(tx: Session) = tx.batchPreparedNamedStatement(query, params)
}

fun List<Int>.krevAtAntallRaderErNøyaktigLik(forventet: Int): List<Int> {
    val sum = sum()
    check(sum == forventet) {
        "Forventet å oppdatere nøyaktig $forventet rader, men endte opp med å oppdatere $sum rader"
    }
    return this
}
