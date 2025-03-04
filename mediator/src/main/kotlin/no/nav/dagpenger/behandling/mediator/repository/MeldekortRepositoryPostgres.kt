package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKorrigeringHendelse

class MeldekortRepositoryPostgres : MeldekortRepository {
    override fun lagre(meldekortHendelse: MeldekortHendelse) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO meldekort_hendelse (ident, meldekort_id, meldingsreferanse_id, orginal_meldekort_id, fom, tom, kilde_ident, kilde_rolle, opprettet)
                    VALUES (:ident, :meldekortId, :meldingReferanseId, :orginalMeldekortId, :fom, :tom, :kildeIdent, :kildeRolle, :opprettet)
                    """.trimIndent(),
                    mapOf(
                        "meldingReferanseId" to meldekortHendelse.meldingsreferanseId(),
                        "meldekortId" to meldekortHendelse.meldekortId,
                        "ident" to meldekortHendelse.ident(),
                        "fom" to meldekortHendelse.fom,
                        "tom" to meldekortHendelse.tom,
                        "orginalMeldekortId" to
                            when (meldekortHendelse) {
                                is MeldekortKorrigeringHendelse -> meldekortHendelse.orginalMeldekortId
                                else -> null
                            },
                        "kildeIdent" to meldekortHendelse.kilde.ident,
                        "kildeRolle" to meldekortHendelse.kilde.rolle,
                        "opprettet" to meldekortHendelse.opprettet,
                    ),
                ).asUpdate,
            )
        }
        meldekortHendelse.dager.forEach { dag ->
            sessionOf(dataSource).use { session ->
                session
                    .run(
                        queryOf(
                            //language=PostgreSQL
                            """
                            INSERT INTO meldekort_dag (id, meldekort_id, dato) 
                            VALUES (:meldekortId, :dato)
                            RETURNING id
                            """.trimIndent(),
                            mapOf(
                                "meldekortId" to meldekortHendelse.meldekortId,
                                "dato" to dag.dato,
                            ),
                        ).asUpdateAndReturnGeneratedKey,
                    ).also { id ->
                        require(id != null) { "Kunne ikke lagre meldekort dag" }
                        dag.aktiviteter.forEach { aktivitet ->
                            session.run(
                                queryOf(
                                    //language=PostgreSQL
                                    """
                                    INSERT INTO meldekort_dag_aktivitet (dag_id, type, timer) 
                                    VALUES (:dagId, :type, :timer)
                                    """.trimIndent(),
                                    mapOf(
                                        "dagId" to id,
                                        "type" to aktivitet.type,
                                        "timer" to aktivitet.timer,
                                    ),
                                ).asUpdate,
                            )
                        }
                    }
            }
        }
    }
}
