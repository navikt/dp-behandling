package no.nav.dagpenger.behandling.mediator

import kotliquery.queryOf
import kotliquery.sessionOf
import mu.KotlinLogging
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.mediator.repository.PostgresUnitOfWork
import no.nav.dagpenger.behandling.mediator.repository.UnitOfWork
import no.nav.dagpenger.uuid.UUIDv7
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Outbox(
    private val rapidsConnection: RapidsConnection,
) : MessageContext {
    private enum class Status {
        Opprettet,
        Sendt,
    }

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val scheduler = Executors.newScheduledThreadPool(1)

    private val task =
        Runnable {
            publiser()
        }

    fun start() {
        scheduler.scheduleAtFixedRate(task, 0, 5, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
            scheduler.shutdownNow()
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                scheduler.shutdown()
                try {
                    stop()
                } catch (e: InterruptedException) {
                    scheduler.shutdownNow()
                }
            },
        )
    }

    private data class OutboxMessage(
        val id: Long,
        val key: String,
        val message: String,
        val opprettet: LocalDateTime,
    )

    private fun publiser() {
        try {
            val meldinger =
                sessionOf(dataSource).use { session ->
                    // language=PostgreSQL
                    session.run(
                        queryOf(
                            """
                            SELECT * FROM outbox WHERE status = :status ORDER BY opprettet
                            """.trimIndent(),
                            mapOf(
                                "status" to Status.Opprettet.name,
                            ),
                        ).map { rad ->
                            OutboxMessage(
                                id = rad.long("id"),
                                key = rad.string("key"),
                                message = rad.string("message"),
                                opprettet = rad.localDateTime("opprettet"),
                            )
                        }.asList,
                    )
                }

            meldinger.onEach { message ->
                logger.info { "Publiserer ${message.message}" }
                rapidsConnection.publish(message.key, message.message)

                sessionOf(dataSource).use { session ->
                    session.run(
                        queryOf(
                            """
                            UPDATE outbox SET status = :status WHERE id = :id
                            """.trimIndent(),
                            mapOf(
                                "id" to message.id,
                                "status" to Status.Sendt.name,
                            ),
                        ).asUpdate,
                    )
                }
            }
            if (meldinger.isNotEmpty()) {
                logger.info { "Publiserte ${meldinger.size} meldinger" }
            }
        } catch (e: Exception) {
            logger.info(e) { "Feil ved publisering av meldinger" }
        }
    }

    fun publish(
        key: String,
        message: String,
        unitOfWork: UnitOfWork<*>,
    ) {
        require(unitOfWork is PostgresUnitOfWork) { "Outbox only supports PostgresUnitOfWork" }
        try {
            unitOfWork.inTransaction { tx ->
                tx.run(
                    // language=PostgreSQL
                    queryOf(
                        "INSERT INTO outbox (key, message, status) VALUES (:key, :message, :status)",
                        paramMap =
                            mapOf(
                                "message" to
                                    PGobject().apply {
                                        type = "json"
                                        value = message
                                    },
                                "key" to key,
                                "status" to Status.Opprettet.name,
                            ),
                    ).asUpdate,
                )
            }
        } catch (e: Exception) {
            TODO("Not yet implemented")
        }
    }

    override fun publish(message: String) {
        publish(UUIDv7.ny().toString(), message, PostgresUnitOfWork.transaction())
    }

    override fun publish(
        key: String,
        message: String,
    ) {
        val unitOfWork = PostgresUnitOfWork.transaction()
        publish(key, message, unitOfWork)
        unitOfWork.commit()
    }

    override fun rapidName(): String = throw IllegalStateException("Outbox does not have a rapid name")
}
