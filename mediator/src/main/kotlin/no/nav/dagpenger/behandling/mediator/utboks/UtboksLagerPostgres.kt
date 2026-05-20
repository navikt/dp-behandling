package no.nav.dagpenger.behandling.mediator.utboks

import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

class UtboksLagerPostgres(
    private val dataSource: DataSource,
) : UtboksLager {
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
