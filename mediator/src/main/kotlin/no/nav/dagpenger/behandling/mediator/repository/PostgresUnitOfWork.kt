package no.nav.dagpenger.behandling.mediator.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Session
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource

class PostgresUnitOfWork private constructor(
    private val session: Session,
) : UnitOfWork<Session> {
    private val transactionTimer = DbMetrics.transactionDuration.startTimer()

    companion object {
        fun transaction() =
            PostgresUnitOfWork(sessionOf(dataSource)).apply {
                session.connection.begin()
                DbMetrics.activeTransactions.inc()
            }

        private val logger = KotlinLogging.logger {}
    }

    override fun commit() {
        val commitTimer = DbMetrics.commitDuration.startTimer()
        try {
            session.connection.commit()
            DbMetrics.commitCounter.inc()
        } finally {
            commitTimer.observeDuration()
            transactionTimer.observeDuration()
            DbMetrics.activeTransactions.dec()
            session.close()
        }
    }

    override fun rollback() = rollbackQuietly()

    override fun <T> inTransaction(block: (Session) -> T): T =
        try {
            block(session)
        } catch (e: Exception) {
            logger.error(e) { "Transaksjonen feilet, ruller tilbake" }
            rollbackQuietly()
            throw e
        }

    private fun rollbackQuietly() {
        val timer = DbMetrics.transactionDuration.startTimer()
        try {
            session.connection.rollback()
            DbMetrics.rollbackCounter.inc()
        } catch (rollbackException: Exception) {
            logger.error(rollbackException) { "Feil under rollback" }
        } finally {
            timer.observeDuration()
            DbMetrics.activeTransactions.dec()
            session.close()
        }
    }
}
