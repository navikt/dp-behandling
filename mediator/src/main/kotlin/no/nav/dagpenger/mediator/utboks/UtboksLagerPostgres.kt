package no.nav.dagpenger.mediator.utboks

import kotliquery.queryOf
import no.nav.dagpenger.mediator.db.DatabaseSession

class UtboksLagerPostgres(
    private val dbSession: DatabaseSession,
) : UtboksLager {
    override fun lagre(melding: String) {
        dbSession.session {
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
