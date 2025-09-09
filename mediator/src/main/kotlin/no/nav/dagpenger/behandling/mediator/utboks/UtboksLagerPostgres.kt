package no.nav.dagpenger.behandling.mediator.utboks

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource

class UtboksLagerPostgres : UtboksLager {
    override fun lagre(melding: String) {
        sessionOf(dataSource).use {
            it.run(
                queryOf(
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
