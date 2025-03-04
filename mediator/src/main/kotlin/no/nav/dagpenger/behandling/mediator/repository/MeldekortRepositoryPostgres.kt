package no.nav.dagpenger.behandling.mediator.repository

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.behandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortHendelse
import no.nav.dagpenger.behandling.modell.hendelser.MeldekortKorrigeringHendelse
import org.postgresql.util.PGobject
import kotlin.time.toJavaDuration

class MeldekortRepositoryPostgres : MeldekortRepository {
    override fun lagre(meldekortHendelse: MeldekortHendelse) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO meldekort_hendelse (id, ident, meldekort_id, meldingsreferanse_id, korrigert_meldekort_id, innsendt_tidspunkt, fom, tom, kilde_ident, kilde_rolle, opprettet)
                    VALUES (:id, :ident, :meldekortId, :meldingReferanseId, :korrigertMeldekortId,  :innsendtTidspunkt, :fom, :tom, :kildeIdent, :kildeRolle, :opprettet)
                    """.trimIndent(),
                    mapOf(
                        "id" to meldekortHendelse.id,
                        "meldingReferanseId" to meldekortHendelse.meldingsreferanseId(),
                        "meldekortId" to meldekortHendelse.meldekortId,
                        "ident" to meldekortHendelse.ident(),
                        "fom" to meldekortHendelse.fom,
                        "tom" to meldekortHendelse.tom,
                        "korrigertMeldekortId" to
                            when (meldekortHendelse) {
                                is MeldekortKorrigeringHendelse -> meldekortHendelse.orginalMeldekortId
                                else -> null
                            },
                        "kildeIdent" to meldekortHendelse.kilde.ident,
                        "kildeRolle" to meldekortHendelse.kilde.rolle,
                        "opprettet" to meldekortHendelse.opprettet,
                        "innsendtTidspunkt" to meldekortHendelse.innsendtTidspunkt,
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
                            INSERT INTO meldekort_dag (meldekort_id, meldt, dato) 
                            VALUES (:meldekortId, :meldt, :dato)
                            """.trimIndent(),
                            mapOf(
                                "meldekortId" to meldekortHendelse.meldekortId,
                                "meldt" to dag.meldt,
                                "dato" to dag.dato,
                            ),
                        ).asUpdate,
                    ).also {
                        dag.aktiviteter.forEach { aktivitet ->
                            session.run(
                                queryOf(
                                    //language=PostgreSQL
                                    """
                                    INSERT INTO meldekort_aktivitet (meldekort_id, dato, type, timer) 
                                    VALUES (:meldekortId, :dato, :type, :timer)
                                    """.trimIndent(),
                                    mapOf(
                                        "meldekortId" to meldekortHendelse.meldekortId,
                                        "dato" to dag.dato,
                                        "type" to aktivitet.type.name,
                                        "timer" to
                                            aktivitet.timer?.toJavaDuration()?.let { timer ->
                                                PGobject().apply {
                                                    type = "interval"
                                                    value = timer.toString()
                                                }
                                            },
                                    ),
                                ).asUpdate,
                            )
                        }
                    }
            }
        }
    }
}
