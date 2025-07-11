package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.Session
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource

class PostgresUnitOfWork private constructor(
    private val session: Session,
) : UnitOfWork<Session> {
    companion object {
        fun transaction() = PostgresUnitOfWork(sessionOf(dataSource)).apply { begin() }
    }

    private fun begin() = session.connection.begin()

    override fun commit() {
        session.connection.commit()
        session.close()
    }

    override fun rollback() {
        session.connection.rollback()
        session.close()
    }

    override fun <T> inTransaction(block: (Session) -> T): T =
        try {
            block(session)
        } catch (e: Exception) {
            // TODO Rollback kan ikke gjøres fordi connection er allerede stengt og maskerer feil mot PostgreSQL
            // rollback()
            session.close()
            throw e
        }
}
