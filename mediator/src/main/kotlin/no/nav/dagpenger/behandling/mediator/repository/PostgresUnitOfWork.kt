package no.nav.dagpenger.behandling.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import kotliquery.sessionOf
import java.sql.Connection
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

data class PostgresUnitOfWork(
    val session: Session,
) {
    companion object {
        fun transaction(
            dataSource: DataSource,
            transactionBlock: PostgresUnitOfWork.() -> Unit,
        ) {
            sessionOf(dataSource).use { session ->
                session.connection.underlying.withTransaction {
                    PostgresUnitOfWork(session).apply(transactionBlock)
                }
            }
        }
    }
}

private fun <R> Connection.withTransaction(transactionBlock: () -> R): R {
    val transactionTimer = DbMetrics.transactionDuration.startTimer()
    val previousValue = autoCommit
    autoCommit = false
    try {
        DbMetrics.activeTransactions.inc()

        val result = transactionBlock()

        commitAndCount()

        return result
    } catch (err: Exception) {
        rollbackAndCount()
        logger.error(err) { "Transaksjonen feilet, ruller tilbake" }
        throw err
    } finally {
        autoCommit = previousValue
        DbMetrics.activeTransactions.dec()
        transactionTimer.observeDuration()
    }
}

private fun Connection.commitAndCount() {
    val commitTimer = DbMetrics.commitDuration.startTimer()
    try {
        commit()
        DbMetrics.commitCounter.inc()
    } finally {
        commitTimer.observeDuration()
    }
}

private fun Connection.rollbackAndCount() {
    val timer = DbMetrics.transactionDuration.startTimer()
    try {
        rollback()
        DbMetrics.rollbackCounter.inc()
    } finally {
        timer.observeDuration()
    }
}
