package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.queryOf
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test

class PostgresUnitOfWorkTest {
    @Test
    fun `rydder opp etter seg og lager ikke connetion leaks`() {
        withMigratedDb {
            repeat(100) {
                runCatching {
                    val unitOfWork = PostgresUnitOfWork.transaction(dataSource)
                    unitOfWork.inTransaction { session ->
                        session.run(queryOf("SELECT 1lasdf!").map { it.int(1) }.asSingle)
                    }
                    unitOfWork.commit()
                }.onFailure { println(it.message) }.onSuccess { throw Error("nien") }
            }
        }
    }
}
