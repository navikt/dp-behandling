package no.nav.dagpenger.behandling.mediator

import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import kotliquery.queryOf
import no.nav.dagpenger.behandling.konfigurasjon.Configuration
import no.nav.dagpenger.behandling.mediator.repository.PostgresUnitOfWork
import no.nav.dagpenger.behandling.mediator.repository.UnitOfWork
import no.nav.dagpenger.uuid.UUIDv7
import no.nav.helse.rapids_rivers.MessageContext
import org.postgresql.util.PGobject

class Outbox : MessageContext {
    fun publish(
        key: String,
        message: String,
        unitOfWork: UnitOfWork<*>,
    ) {
        require(unitOfWork is PostgresUnitOfWork) { "Outbox only supports PostgresUnitOfWork" }
        unitOfWork.inTransaction { tx ->
            tx.run(
                // language=PostgreSQL
                queryOf(
                    "INSERT INTO outbox (key, message) VALUES (:key, :message)",
                    paramMap =
                        mapOf(
                            "message" to
                                PGobject().apply {
                                    type = "json"
                                    value = message
                                },
                            "key" to key,
                        ),
                ).asUpdate,
            )
        }
    }

    override fun publish(message: String) {
        publish(UUIDv7.ny().toString(), message, PostgresUnitOfWork.transaction())
    }

    override fun publish(
        key: String,
        message: String,
    ) {
        publish(key, message, PostgresUnitOfWork.transaction())
    }

    override fun rapidName(): String = Configuration.properties.getOrNull(Key("KAFKA_RAPID_TOPIC", stringType)) ?: "teamdagpenger.rapid.v1"
}
