package no.nav.dagpenger.behandling.mediator.utboks

import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.db.tracedQueryOf

class UtboksLagerPostgres : UtboksLager {
    override fun lagre(melding: String) {
        sessionOf(dataSource).use {
            it.run(
                tracedQueryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO utboks (melding) VALUES (?::jsonb)   
                    """.trimIndent(),
                    melding,
                ).asExecute,
            )
        }
    }
}
