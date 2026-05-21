package no.nav.dagpenger.behandling.mediator.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class PostgresUnitOfWorkTest {
    @Test
    fun `rydder opp etter seg og lager ikke connetion leaks`() {
        withMigratedDb {
            repeat(100) {
                runCatching {
                    PostgresUnitOfWork.transaction(dataSource) {
                        session.run(queryOf("SELECT 1lasdf!").map { it.int(1) }.asSingle)
                    }
                }.onFailure { println(it.message) }.onSuccess { throw Error("nien") }
            }
        }
    }

    @Test
    fun `skal commite når ingen exceptions`() {
        transaksjonstest { dataSource ->
            val commitCountFørTest = DbMetrics.commitCounter.longValue

            PostgresUnitOfWork.transaction(dataSource) {
                DbMetrics.activeTransactions.get() shouldBe 1.0
                session.run(queryOf("update counter set value = 42 where id = 1").asUpdate) shouldBe 1
            }

            DbMetrics.activeTransactions.get() shouldBe 0.0
            (DbMetrics.commitCounter.longValue - commitCountFørTest) shouldBe 1
            sessionOf(dataSource).use { session ->
                session.run(queryOf("select value from counter where id = 1").map { it.int(1) }.asSingle) shouldBe 42
            }
        }
    }

    @Test
    fun `skal rollbacke ved feil`() {
        transaksjonstest { dataSource ->
            val commitCountFørTest = DbMetrics.commitCounter.longValue
            val rollbackCountFørTest = DbMetrics.rollbackCounter.longValue

            shouldThrow<IllegalStateException> {
                PostgresUnitOfWork.transaction(dataSource) {
                    session.run(queryOf("update counter set value = 42 where id = 1").asUpdate) shouldBe 1
                    error("rollback")
                }
            }

            (DbMetrics.commitCounter.longValue - commitCountFørTest) shouldBe 0
            (DbMetrics.rollbackCounter.longValue - rollbackCountFørTest) shouldBe 1
            sessionOf(dataSource).use { session ->
                session.run(queryOf("select value from counter where id = 1").map { it.int(1) }.asSingle) shouldBe 1
            }
        }
    }
}

private fun transaksjonstest(testblokk: (DataSource) -> Unit) {
    withMigratedDb {
        sessionOf(dataSource).use { session ->
            session.run(queryOf("create table counter (id int primary key, value int)").asExecute)
            session.run(queryOf("insert into counter values (1, 1)").asExecute)
        }
        testblokk(dataSource)
    }
}
